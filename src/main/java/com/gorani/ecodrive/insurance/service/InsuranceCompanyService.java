package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceCompany;
import com.gorani.ecodrive.insurance.repository.InsuranceCompanyRepository;
import com.gorani.ecodrive.insurance.domain.InsuranceCompanyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceCompanyService {
    private final InsuranceCompanyRepository insuranceCompanyRepository;

    public List<InsuranceCompany> getCompanies(String status){
        if (status == null){
            return insuranceCompanyRepository.findAll();
        }
        return insuranceCompanyRepository.findAllByStatus(InsuranceCompanyStatus.valueOf(status));
    }

    public InsuranceCompany getCompanyById(Long companyId){
        return insuranceCompanyRepository.findById(companyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_COMPANY_NOT_FOUND));
    }
}
