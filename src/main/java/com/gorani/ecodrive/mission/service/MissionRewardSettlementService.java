package com.gorani.ecodrive.mission.service;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.mission.domain.MissionStatus;
import com.gorani.ecodrive.mission.domain.UserMission;
import com.gorani.ecodrive.mission.repository.UserMissionRepository;
import com.gorani.ecodrive.reward.domain.RewardType;
import com.gorani.ecodrive.reward.service.RewardPointGrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionRewardSettlementService {

    private final UserMissionRepository userMissionRepository;
    private final RewardPointGrantService rewardPointGrantService;

    /**
     * 기간이 종료된 완료 미션의 보상을 정산한다.
     * 일일/주간/월간 모두 periodEndDate 기준으로 동일하게 처리한다.
     */
    @Transactional
    public int settleEndedCompletedMissions() {
        LocalDate today = LocalDate.now(TimeZoneConstants.KST);
        LocalDateTime now = LocalDateTime.now(TimeZoneConstants.KST);

        List<UserMission> targets = userMissionRepository.findRewardTargets(MissionStatus.COMPLETED, today);
        int settledCount = 0;

        for (UserMission mission : targets) {
            String rewardRef = "MISSION:" + mission.getId();
            String description = "미션 달성 보상";

            RewardPointGrantService.RewardGrantResult result = rewardPointGrantService.grantReward(
                    mission.getUser().getId(),
                    RewardType.MISSION,
                    rewardRef,
                    mission.getRewardPointSnapshot(),
                    mission.getPeriodStartDate(),
                    mission.getPeriodEndDate(),
                    String.valueOf(mission.getId()),
                    description
            );

            // 이미 지급된 이력이라도 rewardedAt이 비어 있으면 정합성을 맞춘다.
            if (result != RewardPointGrantService.RewardGrantResult.SKIPPED) {
                mission.markRewarded(now);
                settledCount++;
            }
        }

        log.info("미션 보상 정산 완료. 대상={}, 처리={}", targets.size(), settledCount);
        return settledCount;
    }
}

