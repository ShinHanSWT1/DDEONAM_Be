package com.gorani.ecodrive.coupon.dto;

import java.time.LocalDateTime;

public record CouponCheckoutPrepareResponse(
        String orderId,
        String sessionToken,
        String checkoutUrl,
        Integer amount,
        Integer pointAmount,
        Integer finalAmount,
        LocalDateTime expiresAt
) {
}
