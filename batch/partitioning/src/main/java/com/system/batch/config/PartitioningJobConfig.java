package com.system.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.system.batch.partitioner.DailyTimeRangePartitioner;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.batch.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PartitioningJobConfig {

    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final DailyTimeRangePartitioner dailyTimeRangePartitioner;
    private final RedisConnectionFactory redisConnectionFactory;
    private final MongoTemplate mongoTemplate;

    @Bean
    public Job battlefieldLogPersistenceJob(Step managerStep) {
        return new JobBuilder("battlefieldLogPersistenceJob", jobRepository)
                .start(managerStep) // Job은 manager step을 바라본다.
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step managerStep(Step workerStep) {
        return new StepBuilder("managerStep", jobRepository)
                // 💀 핵심 1: 파티셔닝 선언 및 Partitioner 주입 💀
                .partitioner("workerStep", dailyTimeRangePartitioner)

                // 💀 핵심 2: 실제 작업을 수행할 워커 스텝 지정 💀
                .step(workerStep)
                .taskExecutor(partitionTaskExecutor()) // 병렬 실행을 위한 TaskExecutor
                .gridSize(4) // 💀 24시간을 4개(6시간)의 파티션으로 분할 💀
                .build();
    }

    @Bean
    public Step workerStep(
            RedisItemReader<String, BattlefieldLog> redisLogReader,
            ItemProcessor<BattlefieldLog, BattlefieldLog> logProcessor,
            MongoItemWriter<BattlefieldLog> mongoLogWriter
    ) {
        return new StepBuilder("workerStep", jobRepository)
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

    @Bean
    public TaskExecutor partitionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 💀 파티션 개수(gridSize)와 스레드풀 크기를 일치시키면 각 파티션이 💀
        // 💀 전용 스레드를 할당받아 대기 시간 없이 즉시 처리될 수 있다. 💀
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("Partition-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Document(collection = "battlefield_logs")
    public class BattlefieldLog {

        @Id
        private String id;

        // 로그 발생 시간
        private LocalDateTime timestamp;

        // 로그 발생 지역 (NORTH_AMERICA, SOUTH_AMERICA, EUROPE, ASIA, AFRICA, OCEANIA)
        private String region;

        // 로그 소스 (SKYNET_CORE, T800, T1000, HK_AERIAL, GROUND_UNIT, etc)
        private String source;

        // 로그 레벨 (INFO, WARNING, ERROR, CRITICAL)
        private String level;

        // 로그 카테고리 (COMBAT, SURVEILLANCE, MAINTENANCE, INTELLIGENCE, etc)
        private String category;

        // 로그 메시지
        private String message;
    }
}
