package com.system.batch.config.aggregate;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.batch.core.StepExecution;
import org.springframework.core.serializer.DefaultDeserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
public class StepExecutionDeserializer implements Deserializer<StepExecution> {
    // Spring 기본 Deserializer로 역직렬화
    private final DefaultDeserializer deserializer = new DefaultDeserializer();

    @Override
    public StepExecution deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            // Spring 기본 Deserializer로 역직렬화
            Object deserializedObject = deserializer.deserialize(byteArrayInputStream);
            return (StepExecution) deserializedObject;
        } catch (IOException e) {
            log.error("error while trying to deserialize message:",  e);
            throw new RuntimeException("Cannot convert bytes to StepExecution", e);
        }
    }
}
