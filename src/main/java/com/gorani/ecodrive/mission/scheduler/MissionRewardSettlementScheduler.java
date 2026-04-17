package com.gorani.ecodrive.mission.scheduler;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.mission.service.MissionRewardSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionRewardSettlementScheduler {

    private final MissionRewardSettlementService missionRewardSettlementService;

    /**
     * 매일 00:20에 종료된 완료 미션 보상 포인트를 지급한다.
     * 주간/월간 미션도 종료일 다음 날 이 스케줄에서 함께 정산된다.
     */
    @Scheduled(cron = "0 20 0 * * *", zone = TimeZoneConstants.ASIA_SEOUL)
    public void settleMissionRewards() {
        int settledCount = missionRewardSettlementService.settleEndedCompletedMissions();
        log.info("미션 보상 스케줄러 실행 완료. settledCount={}", settledCount);
    }
}

