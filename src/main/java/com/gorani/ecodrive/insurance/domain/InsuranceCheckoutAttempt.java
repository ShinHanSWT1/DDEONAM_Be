package com.gorani.ecodrive.insurance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_checkout_attempts")
@Getter
@NoArgsConstructor
public class InsuranceCheckoutAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "insurance_contract_id", nullable = false)
    private Long insuranceContractId;

    @Column(name = "user_vehicle_id", nullable = false)
    private Long userVehicleId;

    @Column(name = "insurance_product_id", nullable = false)
    private Long insuranceProductId;

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

    @Column(name = "user_insurance_id")
    private Long userInsuranceId;

    @Column(name = "selected_coverage_ids", length = 1000)
    private String selectedCoverageIds;

    @Column(name = "signature_image", columnDefinition = "TEXT")
    private String signatureImage;

    @Column(length = 255)
    private String email;

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

    public static InsuranceCheckoutAttempt prepare(
            Long userId,
            Long insuranceContractId,
            Long userVehicleId,
            Long insuranceProductId,
            String orderId,
            String sessionToken,
            String title,
            Integer amount,
            Integer pointAmount,
            Integer couponDiscountAmount,
            Integer finalAmount,
            String selectedCoverageIds,
            String signatureImage,
            String email,
            LocalDateTime expiresAt,
            LocalDateTime now
    ) {
        InsuranceCheckoutAttempt attempt = new InsuranceCheckoutAttempt();
        attempt.userId = userId;
        attempt.insuranceContractId = insuranceContractId;
        attempt.userVehicleId = userVehicleId;
        attempt.insuranceProductId = insuranceProductId;
        attempt.orderId = orderId;
        attempt.sessionToken = sessionToken;
        attempt.title = title;
        attempt.amount = amount;
        attempt.pointAmount = pointAmount;
        attempt.couponDiscountAmount = couponDiscountAmount;
        attempt.finalAmount = finalAmount;
        attempt.status = "PREPARED";
        attempt.selectedCoverageIds = selectedCoverageIds;
        attempt.signatureImage = signatureImage;
        attempt.email = email;
        attempt.expiresAt = expiresAt;
        attempt.createdAt = now;
        attempt.updatedAt = now;
        return attempt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public void markCompleted(Long paymentId, Long userInsuranceId, LocalDateTime now) {
        this.status = "COMPLETED";
        this.paymentId = paymentId;
        this.userInsuranceId = userInsuranceId;
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
