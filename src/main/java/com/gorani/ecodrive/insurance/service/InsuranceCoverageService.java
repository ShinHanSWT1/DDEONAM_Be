package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverageStatus;
import com.gorani.ecodrive.insurance.domain.InsurancePlanType;
import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import com.gorani.ecodrive.insurance.repository.InsuranceCoverageRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceCoverageService {

    private final InsuranceCoverageRepository insuranceCoverageRepository;
    private final InsuranceProductRepository insuranceProductRepository;

    public List<InsuranceCoverage> getCoverages(Long productId, String planType, String category) {
        insuranceProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

        InsurancePlanType parsedPlanType = parsePlanType(planType);

        if (parsedPlanType != null && category != null) {
            return insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanTypeAndCategoryAndStatus(
                    productId, parsedPlanType, category, InsuranceCoverageStatus.ACTIVE);
        }
        if (parsedPlanType != null) {
            return insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                    productId, parsedPlanType, InsuranceCoverageStatus.ACTIVE);
        }
        if (category != null) {
            return insuranceCoverageRepository.findAllByInsuranceProduct_IdAndCategoryAndStatus(
                    productId, category, InsuranceCoverageStatus.ACTIVE);
        }
        return insuranceCoverageRepository.findAllByInsuranceProduct_IdAndStatus(productId, InsuranceCoverageStatus.ACTIVE);
    }

    private InsurancePlanType parsePlanType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return InsurancePlanType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_PLAN_TYPE);
        }
    }
    public InsuranceProduct getDiscountPolicy(Long productId) {
        return insuranceProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
    }

}