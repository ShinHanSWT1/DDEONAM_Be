package com.gorani.ecodrive.coupon.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.InternalApiTokenValidator;
import com.gorani.ecodrive.coupon.dto.InternalCouponConsumeRequest;
import com.gorani.ecodrive.coupon.dto.InternalCouponConsumeResponse;
import com.gorani.ecodrive.coupon.dto.InternalCouponPreviewRequest;
import com.gorani.ecodrive.coupon.dto.InternalCouponPreviewResponse;
import com.gorani.ecodrive.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/internal/coupons")
@RequiredArgsConstructor
public class InternalCouponController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final CouponService couponService;
    private final InternalApiTokenValidator internalApiTokenValidator;

    @PostMapping("/use-tokens/preview")
    public ApiResponse<InternalCouponPreviewResponse> previewUseToken(
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Valid @RequestBody InternalCouponPreviewRequest request
    ) {
        internalApiTokenValidator.validate(internalToken);
        log.info("내부 쿠폰 토큰 검증 요청. merchantCode={}, externalOrderId={}", request.merchantCode(), request.externalOrderId());
        return ApiResponse.success(
                "내부 쿠폰 토큰 검증 성공",
                couponService.previewUseToken(request.tokenCode(), request.merchantCode(), request.externalOrderId())
        );
    }

    @PostMapping("/use-tokens/consume")
    public ApiResponse<InternalCouponConsumeResponse> consumeUseToken(
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Valid @RequestBody InternalCouponConsumeRequest request
    ) {
        internalApiTokenValidator.validate(internalToken);
        log.info("내부 쿠폰 토큰 사용 요청. merchantCode={}, externalOrderId={}", request.merchantCode(), request.externalOrderId());
        return ApiResponse.success(
                "내부 쿠폰 토큰 사용 성공",
                couponService.consumeUseToken(request.tokenCode(), request.merchantCode(), request.externalOrderId())
        );
    }
}
