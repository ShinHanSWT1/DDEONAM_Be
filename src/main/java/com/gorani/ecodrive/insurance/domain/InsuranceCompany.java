package com.gorani.ecodrive.insurance.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_companies")
@Getter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class InsuranceCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InsuranceCompanyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
