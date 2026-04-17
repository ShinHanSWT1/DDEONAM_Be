package com.gorani.ecodrive.coupon.dto;

import com.gorani.ecodrive.pay.dto.PayCheckoutResponse;

public record CouponPurchaseResponse(
        UserCouponResponse issuedCoupon,
        PayCheckoutResponse payment
) {
}
