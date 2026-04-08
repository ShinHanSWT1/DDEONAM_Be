package com.gorani.ecodrive.insurance.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "insurance_company_id", nullable = false)
    private InsuranceCompany insuranceCompany;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "base_amount")
    private Integer baseAmount;

    @Column(name = "discount_rate")
    private java.math.BigDecimal discountRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InsuranceProductStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
