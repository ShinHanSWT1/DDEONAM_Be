package com.gorani.ecodrive.coupon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_checkout_attempts")
@Getter
@NoArgsConstructor
public class CouponCheckoutAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Column(name = "order_id", nullable = false, unique = true, length = 80)
    private String orderId;

    @Column(name = "session_token", nullable = false, unique = true, length = 120)
    private String sessionToken;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "point_amount", nullable = false)
    private Integer pointAmount;

    @Column(name = "coupon_discount_amount", nullable = false)
    private Integer couponDiscountAmount;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "issued_coupon_id")
    private Long issuedCouponId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CouponCheckoutAttempt prepare(
            Long userId,
            Long couponTemplateId,
            String orderId,
            String sessionToken,
            String title,
            Integer amount,
            Integer pointAmount,
            Integer couponDiscountAmount,
            Integer finalAmount,
            LocalDateTime expiresAt,
            LocalDateTime now
    ) {
        CouponCheckoutAttempt attempt = new CouponCheckoutAttempt();
        attempt.userId = userId;
        attempt.couponTemplateId = couponTemplateId;
        attempt.orderId = orderId;
        attempt.sessionToken = sessionToken;
        attempt.title = title;
        attempt.amount = amount;
        attempt.pointAmount = pointAmount;
        attempt.couponDiscountAmount = couponDiscountAmount;
        attempt.finalAmount = finalAmount;
        attempt.status = "PREPARED";
        attempt.expiresAt = expiresAt;
        attempt.createdAt = now;
        attempt.updatedAt = now;
        return attempt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public void markCompleted(Long paymentId, Long issuedCouponId, LocalDateTime now) {
        this.status = "COMPLETED";
        this.paymentId = paymentId;
        this.issuedCouponId = issuedCouponId;
        this.confirmedAt = now;
        this.errorMessage = null;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, LocalDateTime now) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.updatedAt = now;
    }
}
