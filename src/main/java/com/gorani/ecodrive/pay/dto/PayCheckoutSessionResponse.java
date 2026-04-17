package com.gorani.ecodrive.pay.dto;

import java.time.LocalDateTime;

public record PayCheckoutSessionResponse(
        String sessionToken,
        String checkoutUrl,
        String status,
        LocalDateTime expiresAt
) {
}
