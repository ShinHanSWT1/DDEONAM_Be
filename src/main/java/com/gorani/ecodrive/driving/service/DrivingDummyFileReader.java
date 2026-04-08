package com.gorani.ecodrive.driving.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gorani.ecodrive.driving.dto.DummyDrivingBatch;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class DrivingDummyFileReader {

    private final ObjectMapper objectMapper;

    public DrivingDummyFileReader() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public DummyDrivingBatch read(Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), DummyDrivingBatch.class);
    }
}
