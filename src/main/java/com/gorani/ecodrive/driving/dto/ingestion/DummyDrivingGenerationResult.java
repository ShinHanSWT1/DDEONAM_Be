package com.gorani.ecodrive.driving.dto.ingestion;

import java.util.List;

public record DummyDrivingGenerationResult(
        int generatedBatches,
        int attemptedUsers,
        List<String> generatedFiles
) {
}
