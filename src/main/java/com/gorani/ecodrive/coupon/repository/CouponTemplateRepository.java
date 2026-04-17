package com.gorani.ecodrive.coupon.repository;

import com.gorani.ecodrive.coupon.domain.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long> {

    List<CouponTemplate> findByStatusOrderByIdAsc(String status);

    Optional<CouponTemplate> findByIdAndStatus(Long id, String status);
}
