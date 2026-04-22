package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.UserInsurance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInsuranceRepository extends JpaRepository<UserInsurance, Long> {

    @EntityGraph(attributePaths = {"insuranceCompany", "insuranceProduct", "insuranceContract"})
    List<UserInsurance> findAllByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"insuranceCompany", "insuranceProduct", "insuranceContract"})
    Optional<UserInsurance> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_Id(Long userId);

    boolean existsByUser_IdAndStatus(Long userId, com.gorani.ecodrive.insurance.domain.UserInsuranceStatus status);

    @EntityGraph(attributePaths = {"insuranceCompany", "insuranceProduct", "insuranceContract"})
    Optional<UserInsurance> findFirstByUser_IdAndUserVehicleIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            Long userVehicleId,
            com.gorani.ecodrive.insurance.domain.UserInsuranceStatus status
    );

    @EntityGraph(attributePaths = {"insuranceCompany", "insuranceProduct", "insuranceContract"})
    Optional<UserInsurance> findFirstByUser_IdAndInsuranceContract_IdAndStatus(
            Long userId,
            Long insuranceContractId,
            com.gorani.ecodrive.insurance.domain.UserInsuranceStatus status
    );
}
