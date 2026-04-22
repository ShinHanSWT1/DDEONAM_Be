package com.gorani.ecodrive.insurance.repository;

import com.gorani.ecodrive.insurance.domain.InsuranceCheckoutAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsuranceCheckoutAttemptRepository extends JpaRepository<InsuranceCheckoutAttempt, Long> {

    Optional<InsuranceCheckoutAttempt> findByUserIdAndOrderId(Long userId, String orderId);
}
