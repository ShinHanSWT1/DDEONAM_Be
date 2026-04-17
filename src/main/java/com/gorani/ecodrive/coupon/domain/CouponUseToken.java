package com.gorani.ecodrive.coupon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_use_tokens")
@Getter
@NoArgsConstructor
public class CouponUseToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_coupon_id", nullable = false)
    private UserCoupon userCoupon;

    @Column(name = "one_time_code", nullable = false, unique = true, length = 64)
    private String oneTimeCode;

    @Column(name = "qr_payload", nullable = false, length = 255)
    private String qrPayload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CouponUseToken issue(UserCoupon userCoupon, String oneTimeCode, String qrPayload, LocalDateTime now, LocalDateTime expiresAt) {
        CouponUseToken token = new CouponUseToken();
        token.userCoupon = userCoupon;
        token.oneTimeCode = oneTimeCode;
        token.qrPayload = qrPayload;
        token.status = "ISSUED";
        token.expiresAt = expiresAt;
        token.createdAt = now;
        return token;
    }

    public void expire(LocalDateTime now) {
        this.status = "EXPIRED";
        this.usedAt = now;
    }
}
