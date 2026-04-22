package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.domain.InsuranceContractStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InsuranceContractRepository extends JpaRepository<InsuranceContract, Long> {

    @EntityGraph(attributePaths = {"insuranceProduct", "insuranceProduct.insuranceCompany"})
    List<InsuranceContract> findAllByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"insuranceProduct", "insuranceProduct.insuranceCompany"})
    List<InsuranceContract> findAllByUser_IdAndStatus(Long userId, InsuranceContractStatus status);

    @EntityGraph(attributePaths = {"insuranceProduct", "insuranceProduct.insuranceCompany", "user"})
    Optional<InsuranceContract> findByIdAndUser_Id(Long id, Long userId);

    Optional<InsuranceContract> findFirstByUser_IdOrderByStartedAtAsc(Long userId);

    @Query("SELECT c FROM InsuranceContract c JOIN FETCH c.user WHERE c.status = 'ACTIVE' AND c.endedAt BETWEEN :from AND :to")
    List<InsuranceContract> findActiveContractsEndingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}