package com.gorani.ecodrive.pay.repository;

import com.gorani.ecodrive.pay.domain.PayChargeAttempt;
import com.gorani.ecodrive.pay.domain.PayChargeAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayChargeAttemptRepository extends JpaRepository<PayChargeAttempt, Long> {
    Optional<PayChargeAttempt> findByOrderId(String orderId);
    Optional<PayChargeAttempt> findByUserIdAndOrderId(Long userId, String orderId);
    List<PayChargeAttempt> findByStatusAndExpiresAtBefore(PayChargeAttemptStatus status, LocalDateTime now);
}
