package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.config.MediaImageProperties;
import com.florastore.web_ban_hoa.config.R2Properties;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import com.florastore.web_ban_hoa.service.LocalMediaStorageSupport;
import com.florastore.web_ban_hoa.service.impl.R2MediaStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class R2MediaStorageServiceTest {

    @Test
    void upload_shouldGenerateKeyCompatibleWithControllerPath() {
        S3Client s3Client = mock(S3Client.class);
        S3Presigner s3Presigner = mock(S3Presigner.class);
        LocalMediaStorageSupport localMediaStorageSupport = mock(LocalMediaStorageSupport.class);

        R2Properties properties = new R2Properties();
        properties.setBucket("flowers");
        properties.setAccountId("acct");

        MediaImageProperties imageProperties = new MediaImageProperties();

        R2MediaStorageService service = new R2MediaStorageService(
                s3Client,
                s3Presigner,
                properties,
                imageProperties,
                localMediaStorageSupport
        );

        byte[] imageBytes = createPngImageBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rose.png",
                "image/png",
                imageBytes
        );

        UploadMediaResponse response = service.upload(file);

        assertThat(response.key()).startsWith("images/");
        assertThat(response.key()).endsWith(".jpg");
        assertThat(response.publicUrl()).contains(response.key());
        assertThat(response.contentType()).isEqualTo("image/jpeg");
        assertThat(response.size()).isGreaterThan(0);
    }

    private byte[] createPngImageBytes() {
        try {
            BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to prepare test image", ex);
        }
    }
}
