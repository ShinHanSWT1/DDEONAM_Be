package com.gorani.ecodrive.driving.scheduler;

import com.gorani.ecodrive.driving.service.ingestion.DrivingDummyAutomationService;
import com.gorani.ecodrive.driving.service.ingestion.DrivingDummyRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrivingDummyScheduler {

    @Value("${app.driving-dummy.generation-enabled:true}")
    private boolean generationEnabled;

    private final DrivingDummyAutomationService drivingDummyAutomationService;
    private final DrivingDummyRefreshService drivingDummyRefreshService;

    @Scheduled(cron = "${app.driving-dummy.refresh-cron:0 0 * * * *}")
    public void refreshDummyDrivingData() {
        log.info("주행 더미데이터 스케줄 실행 시작");
        drivingDummyRefreshService.refreshPendingBatches();
    }

    @Scheduled(cron = "${app.driving-dummy.generation-cron:0 0 6 * * *}")
    public void generateAndRefreshDummyDrivingData() {
        if (!generationEnabled) {
            log.info("주행 더미데이터 자동 생성이 비활성화되어 스킵합니다.");
            return;
        }

        log.info("주행 더미데이터 자동 생성/반영 스케줄 실행 시작");
        drivingDummyAutomationService.generateAndRefresh();
    }
}
