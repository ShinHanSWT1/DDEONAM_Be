package com.gorani.ecodrive.coupon.repository;

import com.gorani.ecodrive.coupon.domain.CouponCheckoutAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponCheckoutAttemptRepository extends JpaRepository<CouponCheckoutAttempt, Long> {

    Optional<CouponCheckoutAttempt> findByUserIdAndOrderId(Long userId, String orderId);
}
