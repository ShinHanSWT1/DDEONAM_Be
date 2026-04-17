package com.gorani.ecodrive.coupon.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutConfirmRequest;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutConfirmResponse;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutPrepareRequest;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutPrepareResponse;
import com.gorani.ecodrive.coupon.dto.CouponPurchaseRequest;
import com.gorani.ecodrive.coupon.dto.CouponPurchaseResponse;
import com.gorani.ecodrive.coupon.dto.CouponTemplateResponse;
import com.gorani.ecodrive.coupon.dto.CouponUseTokenResponse;
import com.gorani.ecodrive.coupon.dto.UserCouponResponse;
import com.gorani.ecodrive.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/templates")
    public ApiResponse<List<CouponTemplateResponse>> getTemplates() {
        log.info("쿠폰 템플릿 조회 API 요청");
        return ApiResponse.success("쿠폰 템플릿 조회 성공", couponService.getActiveTemplates());
    }

    @GetMapping("/templates/{templateId}")
    public ApiResponse<CouponTemplateResponse> getTemplate(@PathVariable Long templateId) {
        log.info("쿠폰 템플릿 상세 조회 API 요청. templateId={}", templateId);
        return ApiResponse.success("쿠폰 템플릿 상세 조회 성공", couponService.getActiveTemplate(templateId));
    }

    @GetMapping("/my")
    public ApiResponse<List<UserCouponResponse>> getMyCoupons(@AuthenticationPrincipal CustomUserPrincipal principal) {
        log.info("내 쿠폰 조회 API 요청. userId={}", principal.getUserId());
        return ApiResponse.success("내 쿠폰 조회 성공", couponService.getUserCoupons(principal.getUserId()));
    }

    @PostMapping("/checkout/prepare")
    public ApiResponse<CouponCheckoutPrepareResponse> prepareCheckout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CouponCheckoutPrepareRequest request
    ) {
        log.info("쿠폰 checkout 준비 API 요청. userId={}, couponTemplateId={}, pointAmount={}",
                principal.getUserId(), request.couponTemplateId(), request.pointAmount());
        return ApiResponse.success(
                "쿠폰 checkout 준비 성공",
                couponService.prepareCheckout(principal.getUserId(), request)
        );
    }

    @PostMapping("/checkout/confirm")
    public ApiResponse<CouponCheckoutConfirmResponse> confirmCheckout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CouponCheckoutConfirmRequest request
    ) {
        log.info("쿠폰 checkout 확정 API 요청. userId={}, orderId={}, paymentId={}, amount={}, status={}",
                principal.getUserId(), request.orderId(), request.paymentId(), request.amount(), request.status());
        return ApiResponse.success(
                "쿠폰 checkout 확정 성공",
                couponService.confirmCheckout(principal.getUserId(), request)
        );
    }

    /**
     * 하위 호환용 즉시 구매 API
     */
    @PostMapping("/purchase")
    public ApiResponse<CouponPurchaseResponse> purchaseCoupon(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CouponPurchaseRequest request
    ) {
        log.info("쿠폰 즉시 구매 API 요청. userId={}, couponTemplateId={}, amount={}, pointAmount={}",
                principal.getUserId(), request.couponTemplateId(), request.amount(), request.pointAmount());
        return ApiResponse.success(
                "쿠폰 즉시 구매 성공",
                couponService.purchaseCoupon(principal.getUserId(), request)
        );
    }

    @PostMapping("/my/{userCouponId}/use-token")
    public ApiResponse<CouponUseTokenResponse> issueCouponUseToken(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userCouponId
    ) {
        log.info("쿠폰 사용 토큰 발급 API 요청. userId={}, userCouponId={}", principal.getUserId(), userCouponId);
        return ApiResponse.success(
                "쿠폰 사용 토큰 발급 성공",
                couponService.issueUseToken(principal.getUserId(), userCouponId)
        );
    }
}
