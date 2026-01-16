package com.system.batch.config.profile.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.system.batch.config.model.BattlefieldLog;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.batch.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Profile("worker")
@EnableBatchIntegration // <--- (1)
@Configuration
@AllArgsConstructor
@Slf4j
public class WorkerConfiguration {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private PlatformTransactionManager transactionManager;
    private RemotePartitioningWorkerStepBuilderFactory remotePartitioningWorkerStepBuilderFactory; // <--- (2)
    private final RedisConnectionFactory redisConnectionFactory;
    private final MongoTemplate mongoTemplate;

    @Bean
    public Step workerStep( // <--- (3) Worker Step 설정 시작
                            QueueChannel inboundRequestsFromManager,
                            DirectChannel outboundRequestsToManager,
                            RedisItemReader<String, BattlefieldLog> redisLogReader,
                            ItemProcessor<BattlefieldLog, BattlefieldLog> logProcessor,
                            MongoItemWriter<BattlefieldLog> mongoLogWriter
    ) {
        return remotePartitioningWorkerStepBuilderFactory.get("workerStep")
                .inputChannel(inboundRequestsFromManager) // <--- (3) 입력 채널 지정!
                .outputChannel(outboundRequestsToManager)
                .<BattlefieldLog, BattlefieldLog>chunk(500, transactionManager)
                .reader(redisLogReader)
                .processor(logProcessor)
                .writer(mongoLogWriter)
                .build();
    }

    @Bean
    @StepScope
    public RedisItemReader<String, BattlefieldLog> redisLogReader(
            @Value("#{stepExecutionContext['startDateTime']}") LocalDateTime startDateTime) {
        return new RedisItemReaderBuilder<String, BattlefieldLog>()
                .redisTemplate(redisTemplate())
                .scanOptions(ScanOptions.scanOptions()
                        .match("logs:" + startDateTime.format(FORMATTER) + ":*")
                        .count(10000)
                        .build())
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<BattlefieldLog, BattlefieldLog> logProcessor() {
        return battlefieldLog -> {
            log.info("Thread: {} - Processing log ID: {}, ",
                    Thread.currentThread().getName(),
                    battlefieldLog.getId());
            return battlefieldLog;
        };
    }

    @Bean
    @StepScope
    public MongoItemWriter<BattlefieldLog> mongoLogWriter() {
        return new MongoItemWriterBuilder<BattlefieldLog>()
                .template(mongoTemplate)
                .mode(MongoItemWriter.Mode.INSERT)
                .build();
    }

    @Bean // <--- (4) 메시지 수신 채널
    public QueueChannel inboundRequestsFromManager() {
        return new QueueChannel();
    }

    @Bean // <--- (5) IntegrationFlow DSL 정의 (카프카 수신 로직)
    public IntegrationFlow inboundFlow(ConsumerFactory<String, String> cf) {
        return IntegrationFlow
                .from(Kafka.messageDrivenChannelAdapter(cf, "remote-partitioning"))
                .channel(inboundRequestsFromManager())
                .get();
    }

    @Bean
    public RedisTemplate<String, BattlefieldLog> redisTemplate() {
        RedisTemplate<String, BattlefieldLog> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); //date 및 dateTime 형태를 직렬화 및 역직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); //문자열 형태로 직렬화 및 역직렬화, 날짜를 문자열배열 및 문자열 형태로 직렬화

        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(mapper, BattlefieldLog.class));
        return redisTemplate;
    }
}
