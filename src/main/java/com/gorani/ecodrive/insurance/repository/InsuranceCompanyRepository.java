package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.InsuranceCompany;
import com.gorani.ecodrive.insurance.domain.InsuranceCompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, Long> {
    List<InsuranceCompany> findAllByStatus(InsuranceCompanyStatus status);
}
