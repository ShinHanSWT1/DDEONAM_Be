package com.gorani.ecodrive.common.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_003", "소셜 로그인에 실패했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    INSURANCE_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_001", "해당 보험사를 찾을 수 없습니다."),
    INSURANCE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_002", "해당 보험 상품을 찾을 수 없습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "올바르지 않은 입력값입니다."),
    INVALID_PLAN_TYPE(HttpStatus.BAD_REQUEST, "COMMON_003", "올바르지 않은 플랜 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}