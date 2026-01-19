package com.system.batch.serdes;

import com.system.batch.model.ResistanceActivity;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.batch.integration.chunk.ChunkRequest;
import org.springframework.batch.integration.chunk.ChunkResponse;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class ChunkResponseSerDes {
    public static class ChunkResponseSerializer implements Serializer<ChunkResponse> {
        private final DefaultSerializer serializeer = new DefaultSerializer();

        @Override
        public byte[] serialize(String topic, ChunkResponse chunkResponse) {
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                serializeer.serialize(chunkResponse, output);
                return output.toByteArray();
            } catch (IOException e) {
                log.error("error while trying to serialize message", e);
                throw new SerializationException("Cannot convert object to bytes", e);
            }
        }
    }

    public static class ChunkResponseDeserializer implements Deserializer<ChunkResponse> {
        private final DefaultDeserializer byteDeserializer = new DefaultDeserializer();

        @Override
        public ChunkResponse deserialize(String topic, byte[] data) {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
                return (ChunkResponse) byteDeserializer.deserialize(byteArrayInputStream);
            } catch (IOException e) {
                log.error("error while deserialize message body:", e);
                throw new SerializationException("Could not convert message body", e);
            }
        }
    }
}

