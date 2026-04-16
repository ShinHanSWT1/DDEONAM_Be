package com.gorani.ecodrive.pay.domain;

import com.gorani.ecodrive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pay_charge_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayChargeAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_id", nullable = false, length = 80, unique = true)
    private String orderId;

    @Column(name = "payment_key", length = 200, unique = true)
    private String paymentKey;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayChargeAttemptStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PayChargeAttempt prepare(User user, String orderId, Integer amount, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        PayChargeAttempt attempt = new PayChargeAttempt();
        attempt.user = user;
        attempt.orderId = orderId;
        attempt.amount = amount;
        attempt.status = PayChargeAttemptStatus.PREPARED;
        attempt.expiresAt = expiresAt;
        attempt.createdAt = now;
        attempt.updatedAt = now;
        return attempt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void markConfirmed(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PayChargeAttemptStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.errorCode = null;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = PayChargeAttemptStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = PayChargeAttemptStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}
