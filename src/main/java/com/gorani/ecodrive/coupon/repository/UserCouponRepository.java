package com.gorani.ecodrive.coupon.repository;

import com.gorani.ecodrive.coupon.domain.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserIdOrderByIdDesc(Long userId);

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);
}
