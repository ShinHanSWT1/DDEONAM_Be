package com.gorani.ecodrive.reward.repository;

import com.gorani.ecodrive.reward.domain.PointRewardGrant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRewardGrantRepository extends JpaRepository<PointRewardGrant, Long> {
    boolean existsByRewardRef(String rewardRef);
}

