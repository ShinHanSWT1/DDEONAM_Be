package com.gorani.ecodrive.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleCustomException - CustomException 처리")
    class HandleCustomExceptionTest {

        @Test
        @DisplayName("INSURANCE_COMPANY_NOT_FOUND: 404 Not Found 반환")
        void handleCustomException_insuranceCompanyNotFound() {
            CustomException ex = new CustomException(ErrorCode.INSURANCE_COMPANY_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().code()).isEqualTo("INSURANCE_001");
            assertThat(response.getBody().message()).isEqualTo("해당 보험사를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("INSURANCE_PRODUCT_NOT_FOUND: 404 Not Found 반환")
        void handleCustomException_insuranceProductNotFound() {
            CustomException ex = new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INSURANCE_002");
            assertThat(response.getBody().message()).isEqualTo("해당 보험 상품을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("INVALID_PLAN_TYPE: 400 Bad Request 반환")
        void handleCustomException_invalidPlanType() {
            CustomException ex = new CustomException(ErrorCode.INVALID_PLAN_TYPE);

            ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("COMMON_003");
            assertThat(response.getBody().message()).isEqualTo("올바르지 않은 플랜 타입입니다.");
        }

        @Test
        @DisplayName("USER_NOT_FOUND: 404 Not Found 반환")
        void handleCustomException_userNotFound() {
            CustomException ex = new CustomException(ErrorCode.USER_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("USER_001");
        }

        @Test
        @DisplayName("응답 body의 success는 항상 false")
        void handleCustomException_responseBodySuccessIsFalse() {
            CustomException ex = new CustomException(ErrorCode.UNAUTHORIZED);

            ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
        }
    }

    @Nested
    @DisplayName("handleIllegalArgumentException - IllegalArgumentException 처리 (신규)")
    class HandleIllegalArgumentExceptionTest {

        @Test
        @DisplayName("IllegalArgumentException 발생 시 400 Bad Request 반환")
        void handleIllegalArgumentException_returns400() {
            IllegalArgumentException ex = new IllegalArgumentException("잘못된 값입니다.");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("IllegalArgumentException 응답 body는 INVALID_INPUT_VALUE 에러코드 포함")
        void handleIllegalArgumentException_responseBodyContainsInvalidInputValue() {
            IllegalArgumentException ex = new IllegalArgumentException("any message");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().code()).isEqualTo("COMMON_002");
            assertThat(response.getBody().message()).isEqualTo("올바르지 않은 입력값입니다.");
        }

        @Test
        @DisplayName("IllegalArgumentException 메시지와 무관하게 항상 INVALID_INPUT_VALUE 에러코드 반환")
        void handleIllegalArgumentException_messageIsIgnored() {
            ResponseEntity<ErrorResponse> response1 = handler.handleIllegalArgumentException(
                    new IllegalArgumentException("message1"));
            ResponseEntity<ErrorResponse> response2 = handler.handleIllegalArgumentException(
                    new IllegalArgumentException("completely different message"));

            assertThat(response1.getBody()).isNotNull();
            assertThat(response2.getBody()).isNotNull();
            assertThat(response1.getBody().code()).isEqualTo(response2.getBody().code());
            assertThat(response1.getBody().message()).isEqualTo(response2.getBody().message());
        }
    }

    @Nested
    @DisplayName("handleException - 일반 Exception 처리")
    class HandleExceptionTest {

        @Test
        @DisplayName("일반 Exception 발생 시 500 Internal Server Error 반환")
        void handleException_returns500() {
            Exception ex = new RuntimeException("예상치 못한 오류");

            ResponseEntity<ErrorResponse> response = handler.handleException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("일반 Exception 응답 body는 INTERNAL_SERVER_ERROR 에러코드 포함")
        void handleException_responseBodyContainsInternalServerError() {
            Exception ex = new RuntimeException("server error");

            ResponseEntity<ErrorResponse> response = handler.handleException(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().code()).isEqualTo("COMMON_001");
            assertThat(response.getBody().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
        }
    }
}