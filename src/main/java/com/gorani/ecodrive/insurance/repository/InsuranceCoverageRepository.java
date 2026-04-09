package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverageStatus;
import com.gorani.ecodrive.insurance.domain.InsurancePlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceCoverageRepository extends JpaRepository<InsuranceCoverage, Long> {
    List<InsuranceCoverage> findAllByInsuranceProduct_IdAndStatus(Long productId, InsuranceCoverageStatus status);
    List<InsuranceCoverage> findAllByInsuranceProduct_IdAndPlanTypeAndStatus(Long productId, InsurancePlanType planType, InsuranceCoverageStatus status);
    List<InsuranceCoverage> findAllByInsuranceProduct_IdAndCategoryAndStatus(Long productId, String category, InsuranceCoverageStatus status);
    List<InsuranceCoverage> findAllByInsuranceProduct_IdAndPlanTypeAndCategoryAndStatus(Long productId, InsurancePlanType planType, String category, InsuranceCoverageStatus status);
    List<InsuranceCoverage> findAllByInsuranceProduct_IdAndIsRequiredAndStatus(Long productId, Boolean isRequired, InsuranceCoverageStatus status);
}