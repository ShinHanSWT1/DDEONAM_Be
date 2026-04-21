package com.gorani.ecodrive.coupon.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalCouponPreviewRequest(
        @NotBlank
        String tokenCode,
        String merchantCode,
        String externalOrderId
) {
}
