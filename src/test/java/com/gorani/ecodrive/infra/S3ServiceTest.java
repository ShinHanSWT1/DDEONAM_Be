package com.gorani.ecodrive.infra;

import com.gorani.ecodrive.infra.s3.S3Config;
import com.gorani.ecodrive.infra.s3.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Import({S3Config.class, S3Service.class})
public class S3ServiceTest {

    @Autowired
    private S3Service s3Service;

    @Test
    @DisplayName("S3 파일 업로드 테스트")
    void uploadTest() throws Exception {
        ClassPathResource resource = new ClassPathResource("images/test.png");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                resource.getInputStream()
        );

        String uploadedUrl = s3Service.upload(file, "test");

        System.out.println("uploadedUrl = " + uploadedUrl);
        assertThat(uploadedUrl).isNotBlank();
    }

    @Test
    void uploadTest2() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test content".getBytes()
        );

        // when
        String imageUrl = s3Service.upload(file, "test/profile");

        // then
        assertThat(imageUrl).isNotBlank();
        assertThat(imageUrl).contains("test/profile");
        System.out.println("Uploaded Image URL: " + imageUrl);
    }

    @Test
    @DisplayName("허용되지 않은 확장자 업로드 시 예외 발생")
    void uploadInvalidExtensionTest() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Service.upload(file, "test/profile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 파일 형식입니다.");
    }


}
