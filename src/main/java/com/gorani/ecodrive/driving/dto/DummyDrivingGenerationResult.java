package com.gorani.ecodrive.driving.dto;

import java.util.List;

public record DummyDrivingGenerationResult(
        int generatedBatches,
        int attemptedUsers,
        List<String> generatedFiles
) {
}
