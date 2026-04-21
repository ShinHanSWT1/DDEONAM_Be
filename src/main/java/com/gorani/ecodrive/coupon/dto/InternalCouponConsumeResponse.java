package com.gorani.ecodrive.coupon.dto;

import java.time.LocalDateTime;

public record InternalCouponConsumeResponse(
        String oneTimeCode,
        Long userCouponId,
        String couponName,
        Integer discountAmount,
        LocalDateTime usedAt
) {
}
