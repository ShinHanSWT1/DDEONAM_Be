package com.gorani.ecodrive.insurance.dto;

import java.time.LocalDateTime;

public record InsuranceCheckoutPrepareResponse(
        Long insuranceContractId,
        String orderId,
        String sessionToken,
        String checkoutUrl,
        Integer amount,
        Integer pointAmount,
        Integer finalAmount,
        LocalDateTime expiresAt
) {
}
