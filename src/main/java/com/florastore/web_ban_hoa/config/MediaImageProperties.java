package com.florastore.web_ban_hoa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "media.image")
public class MediaImageProperties {

    private long maxOriginalSizeBytes = 5L * 1024 * 1024;
    private int maxWidth = 1600;
    private int maxHeight = 1600;
    private double jpegQuality = 0.82;
    private String cacheControl = "public, max-age=31536000, immutable";

    public long getMaxOriginalSizeBytes() {
        return maxOriginalSizeBytes;
    }

    public void setMaxOriginalSizeBytes(long maxOriginalSizeBytes) {
        this.maxOriginalSizeBytes = maxOriginalSizeBytes;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public double getJpegQuality() {
        return jpegQuality;
    }

    public void setJpegQuality(double jpegQuality) {
        this.jpegQuality = jpegQuality;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }
}
