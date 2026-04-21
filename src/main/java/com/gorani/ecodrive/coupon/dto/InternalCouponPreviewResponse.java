package com.gorani.ecodrive.coupon.dto;

import java.time.LocalDateTime;

public record InternalCouponPreviewResponse(
        String oneTimeCode,
        Long userCouponId,
        String couponName,
        Integer discountAmount,
        LocalDateTime tokenExpiresAt
) {
}
