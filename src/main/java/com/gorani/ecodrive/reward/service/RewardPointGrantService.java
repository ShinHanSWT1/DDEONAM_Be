package com.gorani.ecodrive.reward.service;

import com.gorani.ecodrive.pay.service.PayIntegrationService;
import com.gorani.ecodrive.reward.domain.PointRewardGrant;
import com.gorani.ecodrive.reward.domain.RewardType;
import com.gorani.ecodrive.reward.repository.PointRewardGrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardPointGrantService {

    private final PointRewardGrantRepository pointRewardGrantRepository;
    private final PayIntegrationService payIntegrationService;

    /**
     * 리워드 포인트 지급을 수행한다.
     * rewardRef를 멱등 키로 사용하여 동일 보상 중복 지급을 방지한다.
     */
    @Transactional
    public RewardGrantResult grantReward(
            Long userId,
            RewardType rewardType,
            String rewardRef,
            Integer pointAmount,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String sourceRef,
            String description
    ) {
        if (pointAmount == null || pointAmount <= 0) {
            log.info("리워드 포인트 지급 건너뜀(지급 포인트 0). userId={}, rewardType={}, rewardRef={}",
                    userId, rewardType, rewardRef);
            return RewardGrantResult.SKIPPED;
        }

        if (pointRewardGrantRepository.existsByRewardRef(rewardRef)) {
            log.info("리워드 포인트 이미 지급됨. userId={}, rewardType={}, rewardRef={}",
                    userId, rewardType, rewardRef);
            return RewardGrantResult.ALREADY_GRANTED;
        }

        String category = rewardType.name();
        payIntegrationService.earnRewardPoints(
                userId,
                pointAmount,
                category,
                description,
                rewardRef,
                rewardRef
        );

        pointRewardGrantRepository.save(PointRewardGrant.create(
                userId,
                rewardType,
                rewardRef,
                pointAmount,
                periodStartDate,
                periodEndDate,
                sourceRef,
                LocalDateTime.now()
        ));

        log.info("리워드 포인트 지급 완료. userId={}, rewardType={}, rewardRef={}, pointAmount={}",
                userId, rewardType, rewardRef, pointAmount);
        return RewardGrantResult.GRANTED;
    }

    public enum RewardGrantResult {
        GRANTED,
        ALREADY_GRANTED,
        SKIPPED
    }
}

