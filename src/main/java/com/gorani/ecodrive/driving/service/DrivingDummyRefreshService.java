package com.gorani.ecodrive.driving.service;

import com.gorani.ecodrive.driving.dto.DummyDrivingBatch;
import com.gorani.ecodrive.driving.dto.DummyDrivingRefreshResult;
import com.gorani.ecodrive.driving.service.DrivingIngestionService.IngestionSummary;
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
    private final TransactionTemplate transactionTemplate;

    public DummyDrivingRefreshResult refreshPendingBatches() {
        int processedBatches = 0;
        int insertedSessions = 0;
        int insertedEvents = 0;
        int updatedUsers = 0;
        int failedFiles = 0;
        List<String> batchIds = new ArrayList<>();

        try {
            for (Path file : fileManager.listPendingJsonFiles()) {
                try {
                    DummyDrivingBatch batch = fileReader.read(file);
                    FileProcessResult result = transactionTemplate.execute(status -> {
                        IngestionSummary summary = ingestionService.ingest(batch);
                        int updated = aggregationService.refreshSummaries(summary.affectedUserDates());
                        return new FileProcessResult(
                                summary.insertedSessions(),
                                summary.insertedEvents(),
                                updated
                        );
                    });

                    if (result == null) {
                        throw new IllegalStateException("dummy driving batch 처리 결과가 비어 있습니다.");
                    }

                    fileManager.deleteProcessed(file);
                    processedBatches++;
                    insertedSessions += result.insertedSessions();
                    insertedEvents += result.insertedEvents();
                    updatedUsers += result.updatedUsers();
                    batchIds.add(batch.batchId());
                } catch (Exception e) {
                    failedFiles++;
                    try {
                        fileManager.moveToFailed(file);
                    } catch (IOException moveException) {
                        log.error("failed 디렉터리로 파일 이동 실패. file={}", file, moveException);
                    }
                    log.error("dummy driving batch 처리 실패. file={}", file, e);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("pending 파일 목록을 읽는 중 오류가 발생했습니다.", e);
        }

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
            int updatedUsers
    ) {
    }
}
