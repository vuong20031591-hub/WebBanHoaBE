package com.florastore.web_ban_hoa.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(MediaLocalProperties.class)
public class MediaStaticResourceConfig implements WebMvcConfigurer {

    private final MediaLocalProperties mediaLocalProperties;

    public MediaStaticResourceConfig(MediaLocalProperties mediaLocalProperties) {
        this.mediaLocalProperties = mediaLocalProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absoluteStoragePath = Paths.get(mediaLocalProperties.getStoragePath())
                .toAbsolutePath()
                .normalize();

        registry.addResourceHandler("/media/**")
                .addResourceLocations(absoluteStoragePath.toUri().toString());
    }
}
