package com.gorani.ecodrive.driving.service.ingestion;

import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingBatch;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingRefreshResult;
import com.gorani.ecodrive.driving.service.aggregation.DrivingAggregationService;
import com.gorani.ecodrive.driving.service.ingestion.DrivingIngestionService.IngestionSummary;
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
            List<Path> pendingFiles = fileManager.listPendingJsonFiles();
            log.info("Starting pending driving batch refresh. pendingFileCount={}, pendingDir={}", pendingFiles.size(), fileManager.getPendingDir());

            for (Path file : pendingFiles) {
                try {
                    log.info("Processing pending driving batch file. file={}", file);
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
                        throw new IllegalStateException("Dummy driving batch processing returned null result.");
                    }

                    fileManager.deleteProcessed(file);
                    processedBatches++;
                    insertedSessions += result.insertedSessions();
                    insertedEvents += result.insertedEvents();
                    updatedUsers += result.updatedUsers();
                    batchIds.add(batch.batchId());

                    log.info(
                            "Processed pending driving batch file successfully. file={}, batchId={}, insertedSessions={}, insertedEvents={}, updatedUsers={}",
                            file,
                            batch.batchId(),
                            result.insertedSessions(),
                            result.insertedEvents(),
                            result.updatedUsers()
                    );
                } catch (Exception e) {
                    failedFiles++;
                    try {
                        Path failedPath = fileManager.moveToFailed(file);
                        log.warn("Moved failed driving batch file to failed directory. source={}, destination={}", file, failedPath);
                    } catch (IOException moveException) {
                        log.error("Failed to move file to failed directory. file={}", file, moveException);
                    }
                    log.error("Dummy driving batch processing failed. file={}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to list pending driving batch files. pendingDir={}", fileManager.getPendingDir(), e);
            throw new IllegalStateException("Failed to list pending files.", e);
        }

        log.info(
                "Finished pending driving batch refresh. processedBatches={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, failedFiles={}, batchIds={}",
                processedBatches,
                insertedSessions,
                insertedEvents,
                updatedUsers,
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
            int updatedUsers
    ) {
    }
}