package com.gorani.ecodrive.insurance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InsuranceCheckoutConfirmRequest(
        @NotBlank String orderId,
        @NotNull Long paymentId,
        @NotNull @Min(1) Integer amount,
        String status
) {
}
