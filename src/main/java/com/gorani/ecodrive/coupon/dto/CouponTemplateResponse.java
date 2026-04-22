package com.gorani.ecodrive.coupon.dto;

public record CouponTemplateResponse(
        Long id,
        String category,
        String name,
        String discountLabel,
        Integer validDays
) {
}
