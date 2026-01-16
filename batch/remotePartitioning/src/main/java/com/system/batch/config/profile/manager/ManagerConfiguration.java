package com.system.batch.config.profile.manager;

import com.system.batch.partitioner.DailyTimeRangePartitioner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.batch.integration.partition.StepExecutionRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

@Profile("manager")
@EnableBatchIntegration
@Configuration
@AllArgsConstructor
@Slf4j
public class ManagerConfiguration {
    private final JobRepository jobRepository;
    private final RemotePartitioningManagerStepBuilderFactory remotePartitioningManagerStepBuilderFactory;
    private final DailyTimeRangePartitioner dailyTimeRangePartitioner;

    @Bean
    public Job battlefieldLogPersistenceJob(Step managerStep) {
        return new JobBuilder("battlefieldLogPersistenceJob", jobRepository)
                .start(managerStep) // Job은 manager step을 바라본다.
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step managerStep() { // <--- (3) Manager Step 설정 시작
        return remotePartitioningManagerStepBuilderFactory
                .get("managerStep") // <--- (2) 팩토리에서 빌더 얻기
                .partitioner("workerStep", dailyTimeRangePartitioner)
                .outputChannel(outboundRequestsToWorkers()) // <--- (3) 출력 채널 지정!
                .gridSize(4)
                .build();
    }

    @Bean // <--- (3) 메시지 전송 채널 정의
    public DirectChannel outboundRequestsToWorkers() {
        return new DirectChannel();
    }

    @Bean //
    public IntegrationFlow outboundFlow(KafkaTemplate<Long, StepExecutionRequest> kafkaTemplate,
                                        StepExecutionPartitionRouter stepExecutionPartitionRouter) {
        KafkaProducerMessageHandler<Long, StepExecutionRequest> messageHandler =
                new KafkaProducerMessageHandler<>(kafkaTemplate);
        messageHandler.setTopicExpression(new LiteralExpression("remote-partitioning"));
        // 메시지를 보낼 카프카 파티션 결정 로직 주입! (5)
        messageHandler.setPartitionIdExpression(new FunctionExpression<>(
                stepExecutionPartitionRouter::routeToKafkaPartition));
        return IntegrationFlow
                .from(outboundRequestsToWorkers()) // 이 채널로 메시지가 들어오면
                .log() // 로그 한번 찍고
                .handle(messageHandler) // 카프카로 전송!
                .get();
    }

    @Bean // <--- (5) Spring Batch 파티션과 카프카 토픽 파티션 매핑
    public StepExecutionPartitionRouter stepExecutionPartitionRouter() {
        return new StepExecutionPartitionRouter(4);
    }

    // (5)
    public static class StepExecutionPartitionRouter {
        private final int partitionSize;

        public StepExecutionPartitionRouter(int partitionSize) {
            this.partitionSize = partitionSize;
        }

        public Long routeToKafkaPartition(Message<StepExecutionRequest> message) {
            StepExecutionRequest executionRequest = message.getPayload();
            long stepExecutionId = executionRequest.getStepExecutionId();
            long kafkaPartitionId = stepExecutionId % partitionSize;

            log.info("Step Execution Id: {} Kafka Partition Id: {}",
                    stepExecutionId, kafkaPartitionId);

            return kafkaPartitionId;
        }
    }
}