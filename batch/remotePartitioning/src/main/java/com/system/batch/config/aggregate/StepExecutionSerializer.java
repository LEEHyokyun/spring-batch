package com.system.batch.config.aggregate;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.batch.core.StepExecution;
import org.springframework.core.serializer.DefaultSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class StepExecutionSerializer implements Serializer<StepExecution> {
    // Spring 기본 Serializer로 직렬화
    private final DefaultSerializer serializer = new DefaultSerializer();

    @Override
    public byte[] serialize(String topic, StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            serializer.serialize(stepExecution, output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException e) {
            log.error("error while trying to serialize message: ", e);
            throw new SerializationException("Cannot convert StepExecution to bytes", e);
        }
    }
}
