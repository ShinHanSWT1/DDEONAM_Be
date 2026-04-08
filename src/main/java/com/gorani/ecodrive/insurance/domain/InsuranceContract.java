package com.gorani.ecodrive.insurance.domain;

import com.gorani.ecodrive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsuranceContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_product_id", nullable = false)
    private InsuranceProduct insuranceProduct;

    @Column(name = "driving_score_snapshots_id")
    private Long drivingScoreSnapshotId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "contract_period")
    private Integer contractPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 20)
    private InsurancePlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InsuranceContractStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "base_amount")
    private Integer baseAmount;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @Column(name = "discount_rate")
    private BigDecimal discountRate;

    @Column(name = "final_amount")
    private Integer finalAmount;

    @Builder
    public InsuranceContract(User user, InsuranceProduct insuranceProduct,
                             Long drivingScoreSnapshotId, String phoneNumber,
                             String address, Integer contractPeriod,
                             InsurancePlanType planType, InsuranceContractStatus status,
                             LocalDateTime startedAt, LocalDateTime endedAt,
                             LocalDateTime createdAt, Integer baseAmount,
                             Integer discountAmount, BigDecimal discountRate,
                             Integer finalAmount) {
        this.user = user;
        this.insuranceProduct = insuranceProduct;
        this.drivingScoreSnapshotId = drivingScoreSnapshotId;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.contractPeriod = contractPeriod;
        this.planType = planType;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.createdAt = createdAt;
        this.baseAmount = baseAmount;
        this.discountAmount = discountAmount;
        this.discountRate = discountRate;
        this.finalAmount = finalAmount;
    }

    public void cancel() {
        this.status = InsuranceContractStatus.CANCELLED;
    }

    public void activate() {
        this.status = InsuranceContractStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.endedAt = this.startedAt.plusMonths(this.contractPeriod != null ? this.contractPeriod : 12);
    }
}