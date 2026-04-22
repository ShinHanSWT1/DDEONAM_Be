package com.gorani.ecodrive.coupon.repository;

import com.gorani.ecodrive.coupon.domain.CouponUseToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponUseTokenRepository extends JpaRepository<CouponUseToken, Long> {

    List<CouponUseToken> findByUserCouponIdAndStatus(Long userCouponId, String status);

    Optional<CouponUseToken> findByOneTimeCode(String oneTimeCode);
}
