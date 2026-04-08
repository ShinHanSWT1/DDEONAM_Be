package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.domain.InsuranceContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceContractRepository extends JpaRepository<InsuranceContract, Long> {
    List<InsuranceContract> findAllByUser_Id(Long userId);
    List<InsuranceContract> findAllByUser_IdAndStatus(Long userId, InsuranceContractStatus status);
}