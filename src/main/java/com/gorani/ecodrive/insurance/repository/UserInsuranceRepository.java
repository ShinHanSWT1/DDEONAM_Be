package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.UserInsurance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInsuranceRepository extends JpaRepository<UserInsurance, Long> {
    boolean existsByUserVehicleUserId(Long userId);
}
