package com.florastore.web_ban_hoa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true")
    public S3Client r2S3Client(R2Properties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create("https://" + properties.getAccountId() + ".r2.cloudflarestorage.com"))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true")
    public S3Presigner r2S3Presigner(R2Properties properties) {
        return S3Presigner.builder()
                .endpointOverride(URI.create("https://" + properties.getAccountId() + ".r2.cloudflarestorage.com"))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .build();
    }
}
