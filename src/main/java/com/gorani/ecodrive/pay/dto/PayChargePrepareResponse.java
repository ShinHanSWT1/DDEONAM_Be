package com.gorani.ecodrive.pay.dto;

import java.time.LocalDateTime;

public record PayChargePrepareResponse(
        String orderId,
        Integer amount,
        LocalDateTime expiresAt
) {
}
