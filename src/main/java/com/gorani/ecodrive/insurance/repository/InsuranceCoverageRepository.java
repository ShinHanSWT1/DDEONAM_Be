package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
import com.gorani.ecodrive.insurance.domain.InsurancePlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceCoverageRepository extends
        JpaRepository<InsuranceCoverage, Long> {
    List<InsuranceCoverage> findAllByInsuranceProduct_Id(Long
                                                                 productId);
    List<InsuranceCoverage> findAllByInsuranceProduct_IdAndPlanType(Long
                                                                            productId, InsurancePlanType planType);
    List<InsuranceCoverage> findAllByInsuranceProduct_IdAndCategory(Long
                                                                            productId, String category);
    List<InsuranceCoverage>
    findAllByInsuranceProduct_IdAndPlanTypeAndCategory(Long productId,
                                                       InsurancePlanType planType, String category);
}