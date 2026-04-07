package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import com.gorani.ecodrive.insurance.domain.InsuranceProductStatus;
import com.gorani.ecodrive.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceProductService {

    private final InsuranceProductRepository insuranceProductRepository;

    public List<InsuranceProduct> getProducts(Long insuranceCompanyId, String status) {
        if (insuranceCompanyId != null && status != null) {
            return insuranceProductRepository.findAllByInsuranceCompany_IdAndStatus(
                    insuranceCompanyId, InsuranceProductStatus.valueOf(status));
        }
        if (insuranceCompanyId != null) {
            return insuranceProductRepository.findAllByInsuranceCompany_Id(insuranceCompanyId);
        }
        if (status != null) {
            return insuranceProductRepository.findAllByStatus(InsuranceProductStatus.valueOf(status));
        }
        return insuranceProductRepository.findAll();
    }

    public InsuranceProduct getProductById(Long productId) {
        return insuranceProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
    }

    public InsuranceProduct getDiscountPolicy(Long productId) {
        return insuranceProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
    }
}
