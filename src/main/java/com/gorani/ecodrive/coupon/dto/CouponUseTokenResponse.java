package com.gorani.ecodrive.coupon.dto;

import java.time.LocalDateTime;

public record CouponUseTokenResponse(
        Long userCouponId,
        String oneTimeCode,
        String qrPayload,
        LocalDateTime expiresAt
) {
}
