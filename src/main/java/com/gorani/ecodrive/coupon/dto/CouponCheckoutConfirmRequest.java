package com.gorani.ecodrive.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponCheckoutConfirmRequest(
        @NotBlank
        String orderId,
        @NotNull
        Long paymentId,
        @NotNull
        @Min(1)
        Integer amount,
        String status
) {
}
