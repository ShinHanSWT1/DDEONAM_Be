package com.gorani.ecodrive.insurance.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_coverages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsuranceCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_products_id", nullable = false)
    private InsuranceProduct insuranceProduct;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "coverage_name", length = 100)
    private String coverageName;

    @Column(name = "coverage_amount")
    private Integer coverageAmount;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'BASIC'")
    private InsurancePlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private InsuranceCoverageStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}