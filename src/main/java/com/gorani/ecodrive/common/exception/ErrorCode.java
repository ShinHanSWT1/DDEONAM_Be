package com.gorani.ecodrive.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {


    // COMMON
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "?쒕쾭 ?대? ?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "?섎せ???낅젰?낅땲??"),


    // AUTH
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "?몄쬆???꾩슂?⑸땲??"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "?묎렐 沅뚰븳???놁뒿?덈떎."),
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_003", "?뚯뀥 濡쒓렇?몄뿉 ?ㅽ뙣?덉뒿?덈떎."),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "?ъ슜?먮? 李얠쓣 ???놁뒿?덈떎."),

    // VEHICLE
    NO_ACTIVE_VEHICLE(HttpStatus.NOT_FOUND, "VEHICLE_001", "?쒖꽦 ?곹깭 李⑤웾??李얠쓣 ???놁뒿?덈떎."),

    // INSURANCE
    INSURANCE_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_001", "蹂댄뿕?щ? 李얠쓣 ???놁뒿?덈떎."),
    INSURANCE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_002", "蹂댄뿕 ?곹뭹??李얠쓣 ???놁뒿?덈떎."),
    INVALID_PLAN_TYPE(HttpStatus.BAD_REQUEST, "INSURANCE_003", "?섎せ???뚮옖 ??낆엯?덈떎."),
    INSURANCE_CONTRACT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "INSURANCE_004", "?묎렐 沅뚰븳???놁뒿?덈떎."),
    INSURANCE_PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "INSURANCE_005", "?꾩옱 ?먮ℓ 以묒씠 ?꾨땶 ?곹뭹?낅땲??"),
    REQUIRED_COVERAGE_MISSING(HttpStatus.BAD_REQUEST, "INSURANCE_006", "?꾩닔 ?뱀빟???좏깮?섏? ?딆븯?듬땲??"),
    ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "INSURANCE_007", "?대? ?댁???怨꾩빟?낅땲??"),
    USER_INSURANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_008", "?대떦 蹂댄뿕 ?댁뿭??李얠쓣 ???놁뒿?덈떎."),
    CONTRACT_ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, "INSURANCE_009", "?대? ?쒖꽦?붾맂 怨꾩빟?낅땲??"),
    INVALID_CONTRACT_STATUS(HttpStatus.BAD_REQUEST, "INSURANCE_010", "泥섎━?????녿뒗 怨꾩빟 ?곹깭?낅땲??"),
    INSURANCE_CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "INSURANCE_011", "?대떦 蹂댄뿕 怨꾩빟??李얠쓣 ???놁뒿?덈떎."),

    // PAY
    PAY_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_001", "Pay 계좌를 찾을 수 없습니다"),
    PAY_INTEGRATION_FAILED(HttpStatus.BAD_GATEWAY, "PAY_002", "Pay 연동에 실패했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}



