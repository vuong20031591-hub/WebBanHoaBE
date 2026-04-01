package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.config.R2Properties;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import com.florastore.web_ban_hoa.service.impl.R2MediaStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class R2MediaStorageServiceTest {

    @Test
    void upload_shouldGenerateKeyCompatibleWithControllerPath() {
        S3Client s3Client = mock(S3Client.class);
        S3Presigner s3Presigner = mock(S3Presigner.class);

        R2Properties properties = new R2Properties();
        properties.setBucket("flowers");
        properties.setAccountId("acct");

        R2MediaStorageService service = new R2MediaStorageService(s3Client, s3Presigner, properties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rose.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        UploadMediaResponse response = service.upload(file);

        assertThat(response.key()).startsWith("uploads-");
        assertThat(response.key()).doesNotContain("/");
    }
}
