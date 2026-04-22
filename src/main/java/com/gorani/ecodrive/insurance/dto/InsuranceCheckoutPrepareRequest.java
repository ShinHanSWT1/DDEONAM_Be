package com.gorani.ecodrive.insurance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InsuranceCheckoutPrepareRequest(
        @NotNull Long insuranceProductId,
        @NotNull Long userVehicleId,
        @NotBlank String phoneNumber,
        @NotBlank String address,
        @NotNull @Min(1) Integer contractPeriod,
        @NotBlank String planType,
        @NotEmpty List<Long> selectedCoverageIds,
        String signatureImage,
        String email,
        @NotBlank String successUrl,
        @NotBlank String failUrl,
        @Min(0) Integer pointAmount
) {
}
