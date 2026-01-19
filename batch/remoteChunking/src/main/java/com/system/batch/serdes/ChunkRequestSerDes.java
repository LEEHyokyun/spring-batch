package com.system.batch.serdes;

import com.system.batch.model.ResistanceActivity;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.batch.integration.chunk.ChunkRequest;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class ChunkRequestSerDes {
    public static class ChunkRequestSerializer implements Serializer<ChunkRequest<ResistanceActivity>> {
        private final DefaultSerializer serializeer = new DefaultSerializer();

        @Override
        public byte[] serialize(String topic, ChunkRequest<ResistanceActivity> chunkRequest) {
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                serializeer.serialize(chunkRequest, output);
                return output.toByteArray();
            } catch (IOException e) {
                log.error("error while trying to serialize message", e);
                throw new SerializationException("Cannot convert object to bytes", e);
            }
        }
    }

    public static class ChunkRequestDeserializer implements Deserializer<ChunkRequest<ResistanceActivity>> {
        private final DefaultDeserializer byteDeserializer = new DefaultDeserializer();

        @Override
        @SuppressWarnings("unchecked")
        public ChunkRequest<ResistanceActivity> deserialize(String topic, byte[] data) {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
                return (ChunkRequest<ResistanceActivity>) byteDeserializer.deserialize(byteArrayInputStream);
            } catch (IOException e) {
                log.error("error while deserialize message body:", e);
                throw new SerializationException("Could not convert message body", e);
            }
        }
    }
}
