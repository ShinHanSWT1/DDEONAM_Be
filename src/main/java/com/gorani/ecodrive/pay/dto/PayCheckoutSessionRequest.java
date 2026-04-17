package com.gorani.ecodrive.pay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayCheckoutSessionRequest(
        String externalOrderId,
        @NotBlank
        String title,
        @NotNull
        @Min(0)
        Integer amount,
        @Min(0)
        Integer pointAmount,
        @Min(0)
        Integer couponDiscountAmount,
        Long payProductId,
        @NotBlank
        String successUrl,
        @NotBlank
        String failUrl,
        String entryMode,
        String channel
) {
}
