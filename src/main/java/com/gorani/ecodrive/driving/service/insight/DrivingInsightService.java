package com.gorani.ecodrive.driving.service.insight;

import com.gorani.ecodrive.driving.dto.query.DrivingInsightResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingInsightService {

    private final DrivingInsightQueryService queryService;
    private final DrivingInsightRuleEngine ruleEngine;
    private final DrivingInsightPromptFactory promptFactory;
    private final DrivingInsightCache cache;
    private final DrivingInsightGenerationLimiter generationLimiter;
    private final OpenAiDrivingInsightClient openAiClient;

    @Value("${driving-insight.version:insight-v1}")
    private String version;

    public DrivingInsightResponse generate(Long userId, Long userVehicleId) {
        DrivingInsightFeatures features = queryService.loadFeatures(userId, userVehicleId);
        DrivingInsightClassification classification = ruleEngine.classify(features);

        if (classification.style() == DrivingInsightStyle.INSUFFICIENT_DATA) {
            return ruleEngine.fallbackResponse(classification, version, false);
        }

        String cacheKey = cache.key(userId, userVehicleId, features.fingerprint());
        DrivingInsightResponse cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        DrivingInsightResponse response = generateResponse(userId, features, classification);

        cache.put(cacheKey, response);
        return response;
    }

    private DrivingInsightResponse generateResponse(
            Long userId,
            DrivingInsightFeatures features,
            DrivingInsightClassification classification
    ) {
        if (!openAiClient.isEnabled()) {
            return fallback(classification);
        }

        if (!generationLimiter.tryAcquire(userId)) {
            log.info("Driving insight generation limit reached for userId={}", userId);
            return fallback(classification);
        }

        try {
            DrivingInsightResponse response = openAiClient.generate(features, classification, promptFactory, version);
            if (isInvalidAiResponse(response)) {
                log.warn("Driving insight AI response is invalid. styleCode={}", classification.style().code());
                return fallback(classification);
            }
            return response;
        } catch (RuntimeException exception) {
            log.warn("Driving insight AI generation failed. styleCode={}", classification.style().code(), exception);
            return fallback(classification);
        }
    }

    private boolean isInvalidAiResponse(DrivingInsightResponse response) {
        return response.summary() == null || response.summary().isBlank()
                || response.insight() == null || response.insight().isBlank()
                || response.tip() == null || response.tip().isBlank();
    }

    private DrivingInsightResponse fallback(DrivingInsightClassification classification) {
        return ruleEngine.fallbackResponse(classification, version, true);
    }
}
