package com.gorani.ecodrive.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CouponPurchaseRequest(
        @NotNull
        Long couponTemplateId,
        @NotNull
        @Min(1)
        Integer amount,
        @Min(0)
        Integer pointAmount
) {
}
