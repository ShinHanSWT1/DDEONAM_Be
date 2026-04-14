package com.gorani.ecodrive.pay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 결제 요청 DTO
public record PayCheckoutRequest(
        // 결제명
        @NotBlank
        String title,
        // 결제 금액
        @NotNull
        @Min(1)
        Integer amount,
        // 결제 타입
        String paymentType,
        // 포인트 사용 금액
        @Min(0)
        Integer pointAmount,
        // 쿠폰 할인 금액
        @Min(0)
        Integer couponDiscountAmount,
        // Pay 상품 식별자
        Long payProductId
) {
}
