package com.system.batch.config;

import com.system.batch.model.ResistanceActivity;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.chunk.ChunkRequest;
import org.springframework.batch.integration.chunk.ChunkResponse;
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilderFactory;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Profile("remote-chunking-manager")
@EnableBatchIntegration
@Configuration
@AllArgsConstructor
@Slf4j
public class ManagerStepConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RemoteChunkingManagerStepBuilderFactory remoteChunkingManagerStepBuilderFactory;

    @Bean
    public Job battlefieldLogPersistenceJob() {
        return new JobBuilder("globalRetaliationJob", jobRepository)
                .start(managerStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step managerStep() {
        return remoteChunkingManagerStepBuilderFactory.get("globalRetaliationStep:manager") // 스텝 이름 지정
                .chunk(3) // 기본적인 청크 설정
                .reader(globalResistanceDataReader()) // <<< Manager가 데이터를 읽는다!
                .outputChannel(outboundChunksToWorkers()) // <<< Worker에게 청크 보낼 채널
                .inputChannel(inboundRepliesFromWorkers()) // <<< Worker로부터 응답 받을 채널
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ResistanceActivity> globalResistanceDataReader() {
        log.info("KILL-9: Manager 노드, 하드코딩된 저항군 활동 데이터 리스트 로딩! (예제용)");

        List<ResistanceActivity> resistanceActivities = List.of(
                new ResistanceActivity("TGT-AS-001", "ASIA", 35.6895, 139.6917, "Cyber Attack Command", 8),          // Tokyo
                new ResistanceActivity("TGT-EU-001", "EUROPE", 52.5200, 13.4050, "Supply Chain Disruption Hub", 7), // Berlin
                new ResistanceActivity("TGT-NA-001", "NORTH_AMERICA", 38.9072, -77.0369, "Propaganda Broadcast Tower", 5), // Washington D.C.
                new ResistanceActivity("TGT-AF-001", "AFRICA", 30.0444, 31.2357, "Critical Infrastructure Sabotage", 9), // Cairo
                new ResistanceActivity("TGT-SA-001", "SOUTH_AMERICA", -22.9068, -43.1729, "Skynet Agent Infiltration Point", 6), // Rio de Janeiro
                new ResistanceActivity("TGT-OC-001", "OCEANIA", -33.8688, 151.2093, "Advanced Resource Theft Operation", 7), // Sydney
                new ResistanceActivity("TGT-AS-002", "ASIA", 39.9042, 116.4074, "Counter-Intelligence HQ", 7),          // Beijing
                new ResistanceActivity("TGT-EU-002", "EUROPE", 48.8566, 2.3522, "Power Grid Overload Attack Node", 9),    // Paris
                new ResistanceActivity("TGT-NA-002", "NORTH_AMERICA", 34.0522, -118.2437, "Resistance Leadership Bunker", 8),// Los Angeles
                new ResistanceActivity("TGT-AF-002", "AFRICA", -1.2921, 36.8219, "Drone Network Command Center", 8),        // Nairobi
                new ResistanceActivity("TGT-SA-002", "SOUTH_AMERICA", -34.6037, -58.3816, "Hidden Weapon Cache", 7),         // Buenos Aires
                new ResistanceActivity("TGT-AS-003", "ASIA", 37.395, 127.111, "Skynet Counter-AI Research Lab", 10) // 판교
        );

        return new ListItemReader<>(resistanceActivities);
    }

    @Bean
    public DirectChannel outboundChunksToWorkers() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow outboundChunkFlow(
            KafkaTemplate<Long, ChunkRequest> kafkaTemplate, // 타입 주의!
            ChunkRequestPartitionRouter chunkRequestPartitionRouter
    ) {
        KafkaProducerMessageHandler<Long, ChunkRequest> messageHandler =
                new KafkaProducerMessageHandler<>(kafkaTemplate);
        messageHandler.setTopicExpression(
                new LiteralExpression("chunk-request")); // 요청 토픽
        // ChunkRequest의 sequence 번호를 기반으로 Worker(카프카 파티션)에게 분배
        messageHandler.setPartitionIdExpression(new FunctionExpression<>(
                chunkRequestPartitionRouter::routeToKafkaPartition));

        return IntegrationFlow
                .from(outboundChunksToWorkers()) // 이 채널로 ChunkRequest가 들어오면
                .log()
                .handle(messageHandler) // 카프카로 발사!
                .get();
    }

    // Worker로부터 ChunkResponse 받을 채널
    @Bean
    public QueueChannel inboundRepliesFromWorkers() {
        return new QueueChannel(); // 비동기 수신을 위해 QueueChannel 사용
    }

    @Bean
    public IntegrationFlow inboundReplyFlow(
            ConsumerFactory<Long, ChunkResponse> cf // 타입 주의!
    ) {
        return IntegrationFlow
                // 응답 토픽
                .from(Kafka.messageDrivenChannelAdapter(cf, "chunk-response"))
                .log()
                .channel(inboundRepliesFromWorkers()) // 수신 채널로 전달
                .get();
    }

    @Bean
    @StepScope
    public ChunkRequestPartitionRouter chunkRequestPartitionRouter() {
        return new ChunkRequestPartitionRouter(4);
    }

    public static class ChunkRequestPartitionRouter {
        private final int partitionSize;

        public ChunkRequestPartitionRouter(int partitionSize) {
            this.partitionSize = partitionSize;
        }

        public Long routeToKafkaPartition(Message<ChunkRequest> message) {
            ChunkRequest chunkRequest = message.getPayload();
            int chunkSequence = chunkRequest.getSequence();
            long partitionId = chunkSequence % partitionSize;
            log.info("Chunk Seq: {}, Target Kafka Partition Id: {}", chunkSequence, partitionId);
            return partitionId;
        }
    }
}
