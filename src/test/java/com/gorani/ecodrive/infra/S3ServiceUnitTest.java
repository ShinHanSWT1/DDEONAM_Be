package com.gorani.ecodrive.infra;

import com.gorani.ecodrive.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ServiceUnitTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Utilities s3Utilities;

    private S3Service s3Service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws MalformedURLException {
        s3Service = new S3Service(s3Client);
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");

        given(s3Client.utilities()).willReturn(s3Utilities);
        given(s3Utilities.getUrl(any(Consumer.class)))
                .willReturn(new URL("https://test-bucket.s3.amazonaws.com/test/uuid.png"));
    }

    @Test
    @DisplayName("유효한 PNG 파일 업로드 - S3에 putObject 호출 및 URL 반환")
    void upload_validPngFile_callsPutObjectAndReturnsUrl() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "image-bytes".getBytes()
        );

        // when
        String result = s3Service.upload(file, "test");

        // then
        assertThat(result).isNotBlank();
        assertThat(result).contains("test-bucket");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    @DisplayName("유효한 JPG 파일 업로드 성공")
    void upload_validJpgFile_succeeds() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "jpg-bytes".getBytes()
        );

        // when & then
        assertThat(s3Service.upload(file, "images")).isNotBlank();
    }

    @Test
    @DisplayName("유효한 JPEG 파일 업로드 성공")
    void upload_validJpegFile_succeeds() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpeg", "image/jpeg", "jpeg-bytes".getBytes()
        );

        // when & then
        assertThat(s3Service.upload(file, "images")).isNotBlank();
    }

    @Test
    @DisplayName("유효한 WEBP 파일 업로드 성공")
    void upload_validWebpFile_succeeds() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.webp", "image/webp", "webp-bytes".getBytes()
        );

        // when & then
        assertThat(s3Service.upload(file, "images")).isNotBlank();
    }

    @Test
    @DisplayName("빈 파일 업로드 시 IllegalArgumentException 발생")
    void upload_emptyFile_throwsIllegalArgumentException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> s3Service.upload(file, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("업로드할 파일이 없습니다.");
    }

    @Test
    @DisplayName("null 파일 업로드 시 IllegalArgumentException 발생")
    void upload_nullFile_throwsIllegalArgumentException() {
        // when & then
        assertThatThrownBy(() -> s3Service.upload(null, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("업로드할 파일이 없습니다.");
    }

    @Test
    @DisplayName("허용되지 않는 확장자(txt) 업로드 시 IllegalArgumentException 발생")
    void upload_txtExtension_throwsIllegalArgumentException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.txt", "text/plain", "text".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Service.upload(file, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 파일 형식입니다.");
    }

    @Test
    @DisplayName("허용되지 않는 확장자(pdf) 업로드 시 IllegalArgumentException 발생")
    void upload_pdfExtension_throwsIllegalArgumentException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf-data".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Service.upload(file, "docs"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 파일 형식입니다.");
    }

    @Test
    @DisplayName("파일 확장자 없는 경우 IllegalArgumentException 발생")
    void upload_noExtension_throwsIllegalArgumentException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "application/octet-stream", "data".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Service.upload(file, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 확장자가 없습니다.");
    }

    @Test
    @DisplayName("파일 원본명이 null인 경우 IllegalArgumentException 발생")
    void upload_nullOriginalFilename_throwsIllegalArgumentException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "image/png", "data".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Service.upload(file, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원본 파일명이 없습니다.");
    }

    @Test
    @DisplayName("대문자 확장자(PNG)도 허용됨")
    void upload_uppercaseExtension_succeeds() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.PNG", "image/png", "data".getBytes()
        );

        // when & then - should not throw (uppercase is normalized by toLowerCase())
        assertThat(s3Service.upload(file, "images")).isNotBlank();
    }

    @Test
    @DisplayName("업로드 키에 dirName이 접두사로 포함됨")
    void upload_keyContainsDirName() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "jpg-data".getBytes()
        );

        // when
        s3Service.upload(file, "users/profile");

        // then
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertThat(captor.getValue().key()).startsWith("users/profile/");
    }

    @Test
    @DisplayName("파일명 끝에 점(.)만 있는 경우 IllegalArgumentException 발생")
    void upload_filenameEndingWithDot_throwsIllegalArgumentException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.", "image/png", "data".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Service.upload(file, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 확장자가 없습니다.");
    }
}