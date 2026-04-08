package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.UserInsurance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInsuranceRepository extends JpaRepository<UserInsurance, Long> {
    List<UserInsurance> findAllByUser_Id(Long userId);
    Optional<UserInsurance> findByIdAndUser_Id(Long id, Long userId);
    boolean existsByUser_Id(Long userId);
}
