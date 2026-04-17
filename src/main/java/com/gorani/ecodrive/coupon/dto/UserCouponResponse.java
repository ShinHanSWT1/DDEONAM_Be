package com.gorani.ecodrive.coupon.dto;

import java.time.LocalDateTime;

public record UserCouponResponse(
        Long id,
        Long templateId,
        String name,
        String category,
        String discountLabel,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
) {
}
