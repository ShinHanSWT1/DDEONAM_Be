package com.gorani.ecodrive.driving.service.ingestion;

import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingBatch;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingRefreshResult;
import com.gorani.ecodrive.driving.service.aggregation.DrivingAggregationService;
import com.gorani.ecodrive.driving.service.ingestion.DrivingIngestionService.IngestionSummary;
import com.gorani.ecodrive.mission.service.MissionProgressUpdateService;
import com.gorani.ecodrive.mission.service.MissionRewardSettlementService;
import com.gorani.ecodrive.reward.service.CarbonRewardSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingDummyRefreshService {

    private final DrivingDummyFileManager fileManager;
    private final DrivingDummyFileReader fileReader;
    private final DrivingIngestionService ingestionService;
    private final DrivingAggregationService aggregationService;
    private final MissionProgressUpdateService missionProgressUpdateService;
    private final MissionRewardSettlementService missionRewardSettlementService;
    private final CarbonRewardSettlementService carbonRewardSettlementService;
    private final TransactionTemplate transactionTemplate;

    public DummyDrivingRefreshResult refreshPendingBatches() {
        try {
            List<Path> pendingFiles = fileManager.listPendingJsonFiles();
            return refreshPendingBatches(pendingFiles);
        } catch (IOException e) {
            log.error("대기 중인 더미 주행 배치 파일 목록 조회 실패. pendingDir={}", fileManager.getPendingDir(), e);
            throw new IllegalStateException("대기 파일 조회에 실패했습니다.", e);
        }
    }

    public DummyDrivingRefreshResult refreshPendingBatches(List<Path> pendingFiles) {
        int processedBatches = 0;
        int insertedSessions = 0;
        int insertedEvents = 0;
        int updatedUsers = 0;
        int updatedMissions = 0;
        int settledMissionRewards = 0;
        int settledCarbonRewards = 0;
        int failedFiles = 0;
        List<String> batchIds = new ArrayList<>();

        log.info("더미 주행 배치 반영 시작. pendingFileCount={}, pendingDir={}", pendingFiles.size(), fileManager.getPendingDir());

        for (Path file : pendingFiles) {
            try {
                log.info("더미 주행 배치 파일 처리 시작. file={}", file);
                DummyDrivingBatch batch = fileReader.read(file);
                FileProcessResult result = transactionTemplate.execute(status -> {
                    IngestionSummary summary = ingestionService.ingest(batch);
                    int updated = aggregationService.refreshSummaries(summary.affectedUserDates());
                    // 주행 데이터 반영 직후 미션 진행도도 즉시 갱신한다.
                    int missionUpdated = missionProgressUpdateService.refreshProgress(summary.affectedUserDates());
                    // 테스트 편의를 위해 더미 반영 시점에도 보상 정산을 트리거한다.
                    int missionRewardSettled = missionRewardSettlementService.settleEndedCompletedMissions();
                    int carbonRewardSettled = carbonRewardSettlementService.settlePreviousMonth();
                    return new FileProcessResult(
                            summary.insertedSessions(),
                            summary.insertedEvents(),
                            updated,
                            missionUpdated,
                            missionRewardSettled,
                            carbonRewardSettled
                    );
                });

                if (result == null) {
                    throw new IllegalStateException("더미 주행 배치 처리 결과가 비어 있습니다.");
                }

                fileManager.deleteProcessed(file);
                processedBatches++;
                insertedSessions += result.insertedSessions();
                insertedEvents += result.insertedEvents();
                updatedUsers += result.updatedUsers();
                updatedMissions += result.updatedMissions();
                settledMissionRewards += result.settledMissionRewards();
                settledCarbonRewards += result.settledCarbonRewards();
                batchIds.add(batch.batchId());

                log.info(
                        "더미 주행 배치 파일 처리 완료. file={}, batchId={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, updatedMissions={}, settledMissionRewards={}, settledCarbonRewards={}",
                        file,
                        batch.batchId(),
                        result.insertedSessions(),
                        result.insertedEvents(),
                        result.updatedUsers(),
                        result.updatedMissions(),
                        result.settledMissionRewards(),
                        result.settledCarbonRewards()
                );
            } catch (Exception e) {
                failedFiles++;
                try {
                    Path failedPath = fileManager.moveToFailed(file);
                    log.warn("처리 실패 파일을 failed 디렉터리로 이동. source={}, destination={}", file, failedPath);
                } catch (IOException moveException) {
                    log.error("실패 파일 이동 실패. file={}", file, moveException);
                }
                log.error("더미 주행 배치 처리 실패. file={}", file, e);
            }
        }

        log.info(
                "더미 주행 배치 반영 종료. processedBatches={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, updatedMissions={}, settledMissionRewards={}, settledCarbonRewards={}, failedFiles={}, batchIds={}",
                processedBatches,
                insertedSessions,
                insertedEvents,
                updatedUsers,
                updatedMissions,
                settledMissionRewards,
                settledCarbonRewards,
                failedFiles,
                batchIds
        );

        return new DummyDrivingRefreshResult(
                processedBatches,
                insertedSessions,
                insertedEvents,
                updatedUsers,
                failedFiles,
                batchIds
        );
    }

    private record FileProcessResult(
            int insertedSessions,
            int insertedEvents,
            int updatedUsers,
            int updatedMissions,
            int settledMissionRewards,
            int settledCarbonRewards
    ) {
    }
}

