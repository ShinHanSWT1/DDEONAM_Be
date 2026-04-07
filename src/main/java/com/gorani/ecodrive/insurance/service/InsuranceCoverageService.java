package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
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

        if (planType != null && category != null) {
            return insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanTypeAndCategory(
                    productId, InsurancePlanType.valueOf(planType), category);
        }
        if (planType != null) {
            return insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanType(
                    productId, InsurancePlanType.valueOf(planType));
        }
        if (category != null) {
            return insuranceCoverageRepository.findAllByInsuranceProduct_IdAndCategory(
                    productId, category);
        }
        return insuranceCoverageRepository.findAllByInsuranceProduct_Id(productId);
    }
    public InsuranceProduct getDiscountPolicy(Long productId) {
        return insuranceProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
    }

}