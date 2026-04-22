package com.gorani.ecodrive.pay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayChargeConfirmRequest(
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @NotNull @Min(1000) Integer amount
) {
}
