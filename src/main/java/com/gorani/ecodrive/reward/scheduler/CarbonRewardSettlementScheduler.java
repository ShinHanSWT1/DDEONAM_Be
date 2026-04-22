package com.gorani.ecodrive.reward.scheduler;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.reward.service.CarbonRewardSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarbonRewardSettlementScheduler {

    private final CarbonRewardSettlementService carbonRewardSettlementService;

    /**
     * 매월 1일 00:30에 전달 탄소 절감 리워드를 지급한다.
     */
    @Scheduled(cron = "0 30 0 1 * *", zone = TimeZoneConstants.ASIA_SEOUL)
    public void settleCarbonRewards() {
        int settledCount = carbonRewardSettlementService.settlePreviousMonth();
        log.info("탄소 리워드 스케줄러 실행 완료. settledCount={}", settledCount);
    }
}

