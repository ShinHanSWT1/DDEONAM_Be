package com.gorani.ecodrive.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@Getter
@NoArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_template_id", nullable = false)
    private CouponTemplate couponTemplate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_payment_id")
    private Long usedPaymentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static UserCoupon issue(Long userId, CouponTemplate template, LocalDateTime now) {
        UserCoupon coupon = new UserCoupon();
        coupon.userId = userId;
        coupon.couponTemplate = template;
        coupon.status = "AVAILABLE";
        coupon.issuedAt = now;
        coupon.expiresAt = now.plusDays(template.getValidDays());
        coupon.createdAt = now;
        return coupon;
    }

    public void markUsed(LocalDateTime now, Long paymentId) {
        this.status = "USED";
        this.usedAt = now;
        this.usedPaymentId = paymentId;
    }
}
