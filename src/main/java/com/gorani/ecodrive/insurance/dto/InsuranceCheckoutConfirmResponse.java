package com.gorani.ecodrive.insurance.dto;

public record InsuranceCheckoutConfirmResponse(
        String orderId,
        Long paymentId,
        Long insuranceContractId,
        Long userInsuranceId,
        String contractStatus
) {
}
