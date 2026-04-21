package com.gorani.ecodrive.coupon.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalCouponConsumeRequest(
        @NotBlank
        String tokenCode,
        String merchantCode,
        String externalOrderId
) {
}
