package com.gorani.ecodrive.reward.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_reward_grants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointRewardGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    private RewardType rewardType;

    @Column(name = "reward_ref", nullable = false, length = 120, unique = true)
    private String rewardRef;

    @Column(name = "point_amount", nullable = false)
    private Integer pointAmount;

    @Column(name = "period_start_date")
    private LocalDate periodStartDate;

    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    @Column(name = "source_ref", length = 120)
    private String sourceRef;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static PointRewardGrant create(
            Long userId,
            RewardType rewardType,
            String rewardRef,
            Integer pointAmount,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String sourceRef,
            LocalDateTime now
    ) {
        PointRewardGrant grant = new PointRewardGrant();
        grant.userId = userId;
        grant.rewardType = rewardType;
        grant.rewardRef = rewardRef;
        grant.pointAmount = pointAmount;
        grant.periodStartDate = periodStartDate;
        grant.periodEndDate = periodEndDate;
        grant.sourceRef = sourceRef;
        grant.createdAt = now;
        return grant;
    }
}

