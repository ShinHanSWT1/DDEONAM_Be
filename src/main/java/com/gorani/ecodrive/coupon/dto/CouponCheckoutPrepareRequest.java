package com.gorani.ecodrive.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponCheckoutPrepareRequest(
        @NotNull
        Long couponTemplateId,
        @NotBlank
        String successUrl,
        @NotBlank
        String failUrl,
        @Min(0)
        Integer pointAmount
) {
}
