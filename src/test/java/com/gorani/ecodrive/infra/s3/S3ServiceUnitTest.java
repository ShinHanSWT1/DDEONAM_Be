package com.gorani.ecodrive.infra.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceUnitTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;
    private S3Utilities s3Utilities;

    @BeforeEach
    void setUp() throws MalformedURLException {
        s3Service = new S3Service(s3Client);
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");

        // Set up S3Utilities mock for URL generation (lenient to avoid UnnecessaryStubbingException
        // in validation-only tests that never reach the S3 call)
        s3Utilities = mock(S3Utilities.class);
        lenient().when(s3Client.utilities()).thenReturn(s3Utilities);
        lenient().when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new URL("https://test-bucket.s3.ap-northeast-2.amazonaws.com/test/uuid.png"));
    }

    @Nested
    @DisplayName("upload - 파일 업로드")
    class UploadTest {

        @Test
        @DisplayName("유효한 PNG 파일 업로드 성공")
        void upload_validPngFile_returnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", "png content".getBytes());
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            String result = s3Service.upload(file, "test");

            assertThat(result).isNotBlank();
            assertThat(result).startsWith("https://");
        }

        @Test
        @DisplayName("유효한 JPEG 파일 업로드 성공")
        void upload_validJpegFile_returnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpeg", "image/jpeg", "jpeg content".getBytes());
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            String result = s3Service.upload(file, "images");

            assertThat(result).isNotBlank();
        }

        @Test
        @DisplayName("유효한 JPG 파일 업로드 성공")
        void upload_validJpgFile_returnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", "jpg content".getBytes());
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            String result = s3Service.upload(file, "profile");

            assertThat(result).isNotBlank();
        }

        @Test
        @DisplayName("유효한 WEBP 파일 업로드 성공")
        void upload_validWebpFile_returnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.webp", "image/webp", "webp content".getBytes());
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            String result = s3Service.upload(file, "avatars");

            assertThat(result).isNotBlank();
        }

        @Test
        @DisplayName("업로드 시 s3Client.putObject가 한 번 호출됨")
        void upload_callsPutObjectOnce() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", "content".getBytes());
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            s3Service.upload(file, "test");

            verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("파일명은 UUID 기반으로 변경되어 원본 파일명이 아님")
        void upload_storedFileNameIsUuidBased() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "my-original-photo.png", "image/png", "content".getBytes());
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Capture the key used in the request
            final String[] capturedKey = {null};
            doAnswer(invocation -> {
                PutObjectRequest req = invocation.getArgument(0);
                capturedKey[0] = req.key();
                return PutObjectResponse.builder().build();
            }).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

            s3Service.upload(file, "test");

            assertThat(capturedKey[0]).isNotNull();
            assertThat(capturedKey[0]).startsWith("test/");
            assertThat(capturedKey[0]).doesNotContain("my-original-photo");
            assertThat(capturedKey[0]).endsWith(".png");
        }
    }

    @Nested
    @DisplayName("upload - 파일 유효성 검사 실패")
    class UploadValidationTest {

        @Test
        @DisplayName("null 파일이면 IllegalArgumentException 발생")
        void upload_nullFile_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> s3Service.upload(null, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("업로드할 파일이 없습니다.");
        }

        @Test
        @DisplayName("빈 파일이면 IllegalArgumentException 발생")
        void upload_emptyFile_throwsIllegalArgumentException() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> s3Service.upload(emptyFile, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("업로드할 파일이 없습니다.");
        }

        @Test
        @DisplayName("원본 파일명이 없으면 IllegalArgumentException 발생")
        void upload_nullOriginalFilename_throwsIllegalArgumentException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "image/png", "content".getBytes());

            assertThatThrownBy(() -> s3Service.upload(file, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("원본 파일명이 없습니다.");
        }

        @Test
        @DisplayName("TXT 파일 업로드 시 IllegalArgumentException 발생")
        void upload_txtFile_throwsIllegalArgumentException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.txt", "text/plain", "text content".getBytes());

            assertThatThrownBy(() -> s3Service.upload(file, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("허용되지 않는 파일 형식입니다.");
        }

        @Test
        @DisplayName("PDF 파일 업로드 시 IllegalArgumentException 발생")
        void upload_pdfFile_throwsIllegalArgumentException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "pdf content".getBytes());

            assertThatThrownBy(() -> s3Service.upload(file, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("허용되지 않는 파일 형식입니다.");
        }

        @Test
        @DisplayName("GIF 파일 업로드 시 IllegalArgumentException 발생 (허용 목록에 없음)")
        void upload_gifFile_throwsIllegalArgumentException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "animation.gif", "image/gif", "gif content".getBytes());

            assertThatThrownBy(() -> s3Service.upload(file, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("허용되지 않는 파일 형식입니다.");
        }

        @Test
        @DisplayName("확장자 없는 파일명이면 IllegalArgumentException 발생")
        void upload_noExtensionFilename_throwsIllegalArgumentException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "filename_no_extension", "image/png", "content".getBytes());

            assertThatThrownBy(() -> s3Service.upload(file, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일 확장자가 없습니다.");
        }

        @Test
        @DisplayName("점으로 끝나는 파일명이면 IllegalArgumentException 발생")
        void upload_filenameEndingWithDot_throwsIllegalArgumentException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "filename.", "image/png", "content".getBytes());

            assertThatThrownBy(() -> s3Service.upload(file, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일 확장자가 없습니다.");
        }

        @Test
        @DisplayName("대문자 확장자(PNG)도 허용됨")
        void upload_uppercaseExtension_isAllowed() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.PNG", "image/png", "content".getBytes());
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            String result = s3Service.upload(file, "test");

            assertThat(result).isNotBlank();
        }
    }
}