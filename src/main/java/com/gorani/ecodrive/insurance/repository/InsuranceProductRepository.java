package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import com.gorani.ecodrive.insurance.domain.InsuranceProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long>{
    List<InsuranceProduct> findAllByInsuranceCompany_Id(Long insuranceCompanyId);
    List<InsuranceProduct> findAllByStatus(InsuranceProductStatus status);
    List<InsuranceProduct> findAllByInsuranceCompany_IdAndStatus(Long insuraneCompanyId, InsuranceProductStatus status);
}
