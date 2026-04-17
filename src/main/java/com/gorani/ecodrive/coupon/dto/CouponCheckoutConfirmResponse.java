package com.gorani.ecodrive.coupon.dto;

public record CouponCheckoutConfirmResponse(
        String orderId,
        Long paymentId,
        UserCouponResponse issuedCoupon
) {
}
