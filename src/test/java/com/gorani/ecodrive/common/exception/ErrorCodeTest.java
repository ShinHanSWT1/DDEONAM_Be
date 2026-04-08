package com.gorani.ecodrive.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("INVALID_INPUT_VALUE - BAD_REQUEST 상태코드와 COMMON_002 코드를 가짐")
    void invalidInputValue_hasCorrectHttpStatusAndCode() {
        // when
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        // then
        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode.getCode()).isEqualTo("COMMON_002");
        assertThat(errorCode.getMessage()).isEqualTo("잘못된 입력입니다.");
    }

    @Test
    @DisplayName("INVALID_INPUT_VALUE가 ErrorCode enum에 존재함")
    void invalidInputValue_existsInEnum() {
        // when
        ErrorCode[] values = ErrorCode.values();

        // then
        boolean found = false;
        for (ErrorCode code : values) {
            if (code == ErrorCode.INVALID_INPUT_VALUE) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("USER_NOT_FOUND - NOT_FOUND 상태코드를 가짐")
    void userNotFound_hasNotFoundStatus() {
        assertThat(ErrorCode.USER_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.USER_NOT_FOUND.getCode()).isEqualTo("USER_001");
    }

    @Test
    @DisplayName("UNAUTHORIZED - 401 상태코드를 가짐")
    void unauthorized_hasUnauthorizedStatus() {
        assertThat(ErrorCode.UNAUTHORIZED.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.UNAUTHORIZED.getCode()).isEqualTo("AUTH_001");
    }

    @Test
    @DisplayName("INTERNAL_SERVER_ERROR - 500 상태코드를 가짐")
    void internalServerError_hasInternalServerErrorStatus() {
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getCode()).isEqualTo("COMMON_001");
    }

    @Test
    @DisplayName("모든 ErrorCode는 status, code, message를 가짐")
    void allErrorCodes_haveNonNullFields() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.getStatus()).as("status of %s", errorCode.name()).isNotNull();
            assertThat(errorCode.getCode()).as("code of %s", errorCode.name()).isNotBlank();
            assertThat(errorCode.getMessage()).as("message of %s", errorCode.name()).isNotBlank();
        }
    }
}