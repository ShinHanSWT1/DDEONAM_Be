package com.gorani.ecodrive.insurance.domain;

import com.gorani.ecodrive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_insurances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInsurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_vehicle_id", nullable = false)
    private Long userVehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_company_id", nullable = false)
    private InsuranceCompany insuranceCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_product_id")
    private InsuranceProduct insuranceProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_contracts_id", nullable = false)
    private InsuranceContract insuranceContract;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserInsurance(User user, Long userVehicleId, InsuranceCompany insuranceCompany,
                         InsuranceProduct insuranceProduct, InsuranceContract insuranceContract,
                         LocalDateTime createdAt) {
        this.user = user;
        this.userVehicleId = userVehicleId;
        this.insuranceCompany = insuranceCompany;
        this.insuranceProduct = insuranceProduct;
        this.insuranceContract = insuranceContract;
        this.createdAt = createdAt;
    }
}