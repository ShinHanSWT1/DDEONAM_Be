package com.gorani.ecodrive.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // COMMON
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 입력값입니다."),

    // AUTH
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_003", "소셜 로그인에 실패했습니다."),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),

    // VEHICLE
    NO_ACTIVE_VEHICLE(HttpStatus.NOT_FOUND, "VEHICLE_001", "활성 차량을 찾을 수 없습니다."),

    // INSURANCE
    INSURANCE_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_001", "보험사를 찾을 수 없습니다."),
    INSURANCE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_002", "보험 상품을 찾을 수 없습니다."),
    INVALID_PLAN_TYPE(HttpStatus.BAD_REQUEST, "INSURANCE_003", "유효하지 않은 플랜 타입입니다."),
    INSURANCE_CONTRACT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "INSURANCE_004", "해당 보험 계약에 접근할 권한이 없습니다."),
    INSURANCE_PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "INSURANCE_005", "현재 판매 중이 아닌 보험 상품입니다."),
    REQUIRED_COVERAGE_MISSING(HttpStatus.BAD_REQUEST, "INSURANCE_006", "필수 보장 항목이 누락되었습니다."),
    ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "INSURANCE_007", "이미 해지된 계약입니다."),
    USER_INSURANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_008", "사용자 보험 정보를 찾을 수 없습니다."),
    CONTRACT_ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, "INSURANCE_009", "이미 활성 상태인 계약입니다."),
    INVALID_CONTRACT_STATUS(HttpStatus.BAD_REQUEST, "INSURANCE_010", "계약 상태가 유효하지 않습니다."),
    INSURANCE_CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_011", "보험 계약을 찾을 수 없습니다."),

    // PAY
<<<<<<< HEAD
    PAY_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_001", "Pay 계좌를 찾을 수 없습니다"),
    PAY_INTEGRATION_FAILED(HttpStatus.BAD_GATEWAY, "PAY_002", "Pay 연동에 실패했습니다."),

    // NOTIFICATION
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION_002", "해당 알림에 접근 권한이 없습니다.");

=======
    PAY_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_001", "Pay 계좌를 찾을 수 없습니다."),
    PAY_INTEGRATION_FAILED(HttpStatus.BAD_GATEWAY, "PAY_002", "Pay 연동에 실패했습니다."),
    PAY_CHARGE_ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_003", "충전 시도 정보를 찾을 수 없습니다."),
    PAY_CHARGE_ATTEMPT_EXPIRED(HttpStatus.BAD_REQUEST, "PAY_004", "충전 시도가 만료되었습니다."),
    PAY_CHARGE_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAY_005", "충전 금액 정보가 일치하지 않습니다."),
    PAY_CHARGE_DUPLICATED_CONFIRM(HttpStatus.CONFLICT, "PAY_006", "이미 처리된 충전 승인 요청입니다.");
>>>>>>> b371d8ab2f80ba049581c4770dcd300146ac4369

    private final HttpStatus status;
    private final String code;
    private final String message;
}
