package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.config.MediaImageProperties;
import com.florastore.web_ban_hoa.config.R2Properties;
import com.florastore.web_ban_hoa.dto.SignedUrlResponse;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import com.florastore.web_ban_hoa.service.LocalMediaStorageSupport;
import com.florastore.web_ban_hoa.service.MediaStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true")
public class R2MediaStorageService implements MediaStorageService {

    private static final Logger logger = LoggerFactory.getLogger(R2MediaStorageService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final DateTimeFormatter KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Duration DOWNLOAD_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DOWNLOAD_REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(DOWNLOAD_CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final R2Properties properties;
    private final MediaImageProperties imageProperties;
    private final LocalMediaStorageSupport localMediaStorageSupport;
    private final AtomicBoolean r2UploadEnabled;

    public R2MediaStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            R2Properties properties,
            MediaImageProperties imageProperties,
            LocalMediaStorageSupport localMediaStorageSupport
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.imageProperties = imageProperties;
        this.localMediaStorageSupport = localMediaStorageSupport;
        this.r2UploadEnabled = new AtomicBoolean(isR2UploadConfigurationValid());

        if (!this.r2UploadEnabled.get()) {
            logger.warn(
                    "R2 upload is enabled but configuration is invalid (account/bucket/access key). " +
                            "Falling back to local media storage until configuration is corrected"
            );
        }
    }

    @Override
    public UploadMediaResponse upload(MultipartFile file) {
        validateFile(file);

        ProcessedImage processedImage = processImage(readBytes(file));
        return uploadProcessedImage(processedImage);
    }

    @Override
    public UploadMediaResponse uploadFromUrl(String imageUrl) {
        URI uri = parseAndValidateImageUrl(imageUrl);
        RemoteImage remoteImage = downloadRemoteImage(uri);
        validateContentTypeIfPresent(remoteImage.contentType());

        ProcessedImage processedImage = processImage(remoteImage.bytes());
        return uploadProcessedImage(processedImage);
    }

    private UploadMediaResponse uploadProcessedImage(ProcessedImage processedImage) {
        if (!r2UploadEnabled.get()) {
            return localMediaStorageSupport.upload(processedImage.bytes(), processedImage.contentType());
        }

        String key = buildKey();

        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(processedImage.contentType())
                    .contentLength((long) processedImage.bytes().length);

            String cacheControl = safeTrim(imageProperties.getCacheControl());
            if (!cacheControl.isEmpty()) {
                requestBuilder.cacheControl(cacheControl);
            }

            PutObjectRequest request = requestBuilder.build();

            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromBytes(processedImage.bytes()));

            String publicUrl = buildPublicUrl(key);
            return new UploadMediaResponse(key, publicUrl, processedImage.bytes().length, processedImage.contentType());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (S3Exception ex) {
            if (isInvalidAccessKeyError(ex)) {
                if (r2UploadEnabled.compareAndSet(true, false)) {
                    logger.warn(
                            "Detected invalid R2 credentials while uploading. " +
                                    "Disabling R2 upload for this runtime and falling back to local media storage"
                    );
                }
            } else {
                logger.error("Failed to upload processed image to R2 bucket {}", properties.getBucket(), ex);
            }
            return fallbackToLocalStorage(processedImage, ex);
        } catch (Exception ex) {
            logger.error("Failed to upload processed image to R2 bucket {}", properties.getBucket(), ex);
            return fallbackToLocalStorage(processedImage, ex);
        }
    }

    @Override
    public void delete(String key) {
        if (localMediaStorageSupport.supports(key)) {
            localMediaStorageSupport.delete(key);
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to delete file from R2", ex);
        }
    }

    @Override
    public SignedUrlResponse signedUrl(String key, long expiresInSeconds) {
        if (localMediaStorageSupport.supports(key)) {
            return localMediaStorageSupport.signedUrl(key, expiresInSeconds);
        }

        if (expiresInSeconds <= 0) {
            expiresInSeconds = 600;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofSeconds(expiresInSeconds))
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            return new SignedUrlResponse(key, url, expiresInSeconds);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to generate signed URL", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        long maxOriginalSizeBytes = imageProperties.getMaxOriginalSizeBytes();
        if (maxOriginalSizeBytes > 0 && file.getSize() > maxOriginalSizeBytes) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File size exceeds " + maxOriginalSizeBytes + " bytes"
            );
        }

        validateContentTypeIfPresent(file.getContentType());
    }

    private void validateContentTypeIfPresent(String rawContentType) {
        String contentType = normalizeContentType(rawContentType);
        if (contentType != null && !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read image data", ex);
        }
    }

    private URI parseAndValidateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageUrl is required");
        }

        URI uri;
        try {
            uri = URI.create(imageUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image URL", ex);
        }

        String scheme = safeTrim(uri.getScheme()).toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only http/https image URLs are supported");
        }

        String host = safeTrim(uri.getHost()).toLowerCase(Locale.ROOT);
        if (host.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image URL host");
        }

        if ("localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host) || "::1".equals(host)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Localhost image URLs are not allowed");
        }

        return uri;
    }

    private RemoteImage downloadRemoteImage(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(DOWNLOAD_REQUEST_TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Image URL returned HTTP " + statusCode
                );
            }

            String contentType = normalizeContentType(response.headers().firstValue("Content-Type").orElse(null));
            byte[] bytes = readStreamWithLimit(response.body(), imageProperties.getMaxOriginalSizeBytes());

            if (bytes.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Downloaded image is empty");
            }

            return new RemoteImage(bytes, contentType);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to fetch image from URL", ex);
        }
    }

    private byte[] readStreamWithLimit(InputStream inputStream, long maxBytes) throws IOException {
        long effectiveMaxBytes = maxBytes > 0 ? maxBytes : 5L * 1024 * 1024;
        try (InputStream stream = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                if (output.size() + read > effectiveMaxBytes) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "File size exceeds " + effectiveMaxBytes + " bytes"
                    );
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private ProcessedImage processImage(byte[] sourceBytes) {
        try {
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceBytes));
            if (sourceImage == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
            }

            BufferedImage normalized = toRgbImage(sourceImage);
            BufferedImage resized = resizeIfNeeded(normalized);
            byte[] encoded = encodeJpeg(resized);

            return new ProcessedImage(encoded, "image/jpeg");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read image data", ex);
        }
    }

    private BufferedImage toRgbImage(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }

        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, converted.getWidth(), converted.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    private BufferedImage resizeIfNeeded(BufferedImage source) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int maxWidth = Math.max(1, imageProperties.getMaxWidth());
        int maxHeight = Math.max(1, imageProperties.getMaxHeight());

        double widthRatio = (double) maxWidth / sourceWidth;
        double heightRatio = (double) maxHeight / sourceHeight;
        double scale = Math.min(1.0d, Math.min(widthRatio, heightRatio));

        if (scale >= 1.0d) {
            return source;
        }

        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JPEG writer is not available");
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality((float) clampJpegQuality(imageProperties.getJpegQuality()));
            }

            writer.write(null, new IIOImage(image, null, null), writeParam);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to encode image", ex);
        } finally {
            writer.dispose();
        }
    }

    private double clampJpegQuality(double quality) {
        return Math.max(0.1d, Math.min(1.0d, quality));
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        return contentType.toLowerCase(Locale.ROOT)
                .split(";")[0]
                .trim();
    }

    private String buildKey() {
        return "images/" + LocalDate.now().format(KEY_DATE_FORMATTER) + "/" + UUID.randomUUID() + ".jpg";
    }

    private String buildPublicUrl(String key) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return properties.getPublicBaseUrl().replaceAll("/$", "") + "/" + key;
        }
        return "https://" + properties.getAccountId() + ".r2.cloudflarestorage.com/" + properties.getBucket() + "/" + key;
    }

    private UploadMediaResponse fallbackToLocalStorage(ProcessedImage processedImage, Exception ex) {
        if (!r2UploadEnabled.get()) {
            return localMediaStorageSupport.upload(processedImage.bytes(), processedImage.contentType());
        }

        if (isInvalidAccessKeyError(ex)) {
            logger.warn("Falling back to local media storage because R2 credentials are invalid");
        } else {
            logger.warn("Falling back to local media storage because R2 upload failed: {}", ex.getMessage());
        }

        return localMediaStorageSupport.upload(processedImage.bytes(), processedImage.contentType());
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isInvalidAccessKeyError(Exception ex) {
        String message = "";
        if (ex instanceof S3Exception s3Exception
                && s3Exception.awsErrorDetails() != null
                && s3Exception.awsErrorDetails().errorMessage() != null) {
            message = s3Exception.awsErrorDetails().errorMessage();
        } else if (ex.getMessage() != null) {
            message = ex.getMessage();
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("credential access key has length")
                || normalized.contains("access key id") && normalized.contains("invalid");
    }

    private boolean isR2UploadConfigurationValid() {
        String accountId = safeTrim(properties.getAccountId());
        String bucket = safeTrim(properties.getBucket());
        String accessKey = safeTrim(properties.getAccessKey());

        return !accountId.isEmpty() && !bucket.isEmpty() && accessKey.length() == 32;
    }

    private record RemoteImage(byte[] bytes, String contentType) {
    }

    private record ProcessedImage(byte[] bytes, String contentType) {
    }
}
