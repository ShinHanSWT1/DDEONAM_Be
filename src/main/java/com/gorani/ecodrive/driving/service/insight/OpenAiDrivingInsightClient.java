package com.gorani.ecodrive.driving.service.insight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gorani.ecodrive.driving.dto.query.DrivingInsightResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class OpenAiDrivingInsightClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;

    public OpenAiDrivingInsightClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.timeout-seconds:10}") long timeoutSeconds
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    public boolean isEnabled() {
        return !apiKey.isBlank();
    }

    public DrivingInsightResponse generate(
            DrivingInsightFeatures features,
            DrivingInsightClassification classification,
            DrivingInsightPromptFactory promptFactory,
            String version
    ) {
        if (!isEnabled()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }

        try {
            ObjectNode properties = objectMapper.createObjectNode();
            properties.set("summary", objectMapper.createObjectNode().put("type", "string"));
            properties.set("insight", objectMapper.createObjectNode().put("type", "string"));
            properties.set("tip", objectMapper.createObjectNode().put("type", "string"));
            properties.set("confidence", objectMapper.createObjectNode().put("type", "number"));

            ArrayNode required = objectMapper.createArrayNode();
            required.add("summary");
            required.add("insight");
            required.add("tip");
            required.add("confidence");

            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "object");
            schema.set("properties", properties);
            schema.set("required", required);
            schema.put("additionalProperties", false);

            ObjectNode format = objectMapper.createObjectNode();
            format.put("type", "json_schema");
            format.put("name", "driving_insight_payload");
            format.put("strict", true);
            format.set("schema", schema);

            ObjectNode text = objectMapper.createObjectNode();
            text.set("format", format);

            ArrayNode input = objectMapper.createArrayNode();
            input.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", promptFactory.systemPrompt()));
            input.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", promptFactory.userPrompt(features, classification)));

            ObjectNode requestPayload = objectMapper.createObjectNode();
            requestPayload.put("model", model);
            requestPayload.set("input", input);
            requestPayload.set("text", text);

            String body = objectMapper.writeValueAsString(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/responses"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI API returned " + response.statusCode());
            }

            return toInsightResponse(
                    classification,
                    version,
                    objectMapper.readTree(response.body())
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("OpenAI driving insight generation failed", e);
            throw new IllegalStateException("OpenAI driving insight generation failed", e);
        }
    }

    private DrivingInsightResponse toInsightResponse(
            DrivingInsightClassification classification,
            String version,
            JsonNode root
    ) throws IOException {
        String jsonText = extractOutputText(root);
        JsonNode payload = objectMapper.readTree(jsonText);

        return new DrivingInsightResponse(
                classification.style().code(),
                classification.style().label(),
                sanitizeText(payload.path("summary").asText()),
                sanitizeText(payload.path("insight").asText()),
                sanitizeText(payload.path("tip").asText()),
                normalizeConfidence(payload.path("confidence").asDouble(0.75)),
                version,
                false
        );
    }

    private String extractOutputText(JsonNode root) {
        JsonNode outputs = root.path("output");
        if (!outputs.isArray()) {
            throw new IllegalStateException("OpenAI response missing output array");
        }

        for (JsonNode output : outputs) {
            JsonNode contents = output.path("content");
            if (!contents.isArray()) {
                continue;
            }
            for (JsonNode content : contents) {
                if ("output_text".equals(content.path("type").asText()) && content.hasNonNull("text")) {
                    return content.path("text").asText();
                }
            }
        }

        throw new IllegalStateException("OpenAI response missing output_text");
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replaceAll("\\s+", " ")
                .replace("과속 비율", "과속 이벤트 비중")
                .replace("과속률", "과속 이벤트 비중");
    }

    private double normalizeConfidence(double confidence) {
        return Math.max(0.0, Math.min(confidence, 1.0));
    }
}
