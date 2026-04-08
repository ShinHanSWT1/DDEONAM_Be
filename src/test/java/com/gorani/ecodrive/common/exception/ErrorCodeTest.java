package com.gorani.ecodrive.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode - 에러코드 정의 검증")
class ErrorCodeTest {

    @Test
    @DisplayName("INSURANCE_COMPANY_NOT_FOUND: 404, INSURANCE_001, 메시지 확인")
    void insuranceCompanyNotFound() {
        ErrorCode code = ErrorCode.INSURANCE_COMPANY_NOT_FOUND;

        assertThat(code.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(code.getCode()).isEqualTo("INSURANCE_001");
        assertThat(code.getMessage()).isEqualTo("해당 보험사를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("INSURANCE_PRODUCT_NOT_FOUND: 404, INSURANCE_002, 메시지 확인")
    void insuranceProductNotFound() {
        ErrorCode code = ErrorCode.INSURANCE_PRODUCT_NOT_FOUND;

        assertThat(code.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(code.getCode()).isEqualTo("INSURANCE_002");
        assertThat(code.getMessage()).isEqualTo("해당 보험 상품을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("INVALID_INPUT_VALUE: 400, COMMON_002, 메시지 확인")
    void invalidInputValue() {
        ErrorCode code = ErrorCode.INVALID_INPUT_VALUE;

        assertThat(code.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(code.getCode()).isEqualTo("COMMON_002");
        assertThat(code.getMessage()).isEqualTo("올바르지 않은 입력값입니다.");
    }

    @Test
    @DisplayName("INVALID_PLAN_TYPE: 400, COMMON_003, 메시지 확인")
    void invalidPlanType() {
        ErrorCode code = ErrorCode.INVALID_PLAN_TYPE;

        assertThat(code.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(code.getCode()).isEqualTo("COMMON_003");
        assertThat(code.getMessage()).isEqualTo("올바르지 않은 플랜 타입입니다.");
    }

    @Test
    @DisplayName("ErrorResponse.of()로 올바른 ErrorResponse 생성 - INSURANCE_COMPANY_NOT_FOUND")
    void errorResponseOf_insuranceCompanyNotFound() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INSURANCE_COMPANY_NOT_FOUND);

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("INSURANCE_001");
        assertThat(response.message()).isEqualTo("해당 보험사를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("ErrorResponse.of()로 올바른 ErrorResponse 생성 - INVALID_PLAN_TYPE")
    void errorResponseOf_invalidPlanType() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_PLAN_TYPE);

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("COMMON_003");
        assertThat(response.message()).isEqualTo("올바르지 않은 플랜 타입입니다.");
    }

    @Test
    @DisplayName("INSURANCE_COMPANY_NOT_FOUND와 INSURANCE_PRODUCT_NOT_FOUND는 서로 다른 코드")
    void insuranceErrorCodesAreDistinct() {
        assertThat(ErrorCode.INSURANCE_COMPANY_NOT_FOUND.getCode())
                .isNotEqualTo(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("INVALID_INPUT_VALUE와 INVALID_PLAN_TYPE은 모두 BAD_REQUEST이지만 다른 코드")
    void badRequestErrorCodesAreDistinct() {
        assertThat(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .isEqualTo(ErrorCode.INVALID_PLAN_TYPE.getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.INVALID_INPUT_VALUE.getCode())
                .isNotEqualTo(ErrorCode.INVALID_PLAN_TYPE.getCode());
    }
}