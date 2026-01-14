package com.system.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.system.batch.partitioner.DailyTimeRangePartitioner;
import com.system.batch.partitioner.FlatFilePartitioner;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.batch.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
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
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlatFilePartitioningJobConfig {

    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final DailyTimeRangePartitioner dailyTimeRangePartitioner;
    private final Partitioner flatFilePartitioner;
    private final RedisConnectionFactory redisConnectionFactory;
    private final MongoTemplate mongoTemplate;

    @Bean
    public Job battlefieldLogPersistenceJob(Step managerStep, Step mergeOutputFilesStep) {
        return new JobBuilder("battlefieldLogPersistenceJob", jobRepository)
                .start(managerStep) // Job은 manager step을 바라본다.
                .next(mergeOutputFilesStep)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step managerStep(Step workerStep) {
        return new StepBuilder("managerStep", jobRepository)
                // 💀 핵심 1: 파티셔닝 선언 및 Partitioner 주입 💀
                .partitioner("workerStep", flatFilePartitioner)

                // 💀 핵심 2: 실제 작업을 수행할 워커 스텝 지정 💀
                .step(workerStep)
                .taskExecutor(partitionTaskExecutor()) // 병렬 실행을 위한 TaskExecutor
                //.gridSize(4) // flat file -> grid size 의미없음
                .build();
    }

    @Bean
    public Step workerStep(
            FlatFileItemReader<BattlefieldLog> battlefieldLogReader,
            ItemProcessor<BattlefieldLog, BattlefieldLog> logProcessor,
            FlatFileItemWriter<BattlefieldLog> battlefieldLogFileWriter
    ) {
        return new StepBuilder("workerStep", jobRepository)
                .<BattlefieldLog, BattlefieldLog>chunk(100, transactionManager)
                .reader(battlefieldLogReader)
                .processor(logProcessor)
                .writer(battlefieldLogFileWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<BattlefieldLog> battlefieldLogReader(
            @Value("#{stepExecutionContext['fileName']}") String fileName
    ) {
        ResourcePatternResolver resourceLoader =
                new PathMatchingResourcePatternResolver();

        return new FlatFileItemReaderBuilder<BattlefieldLog>()
                .name("battlefieldLogReader")
                .resource(resourceLoader.getResource(fileName))
                .linesToSkip(1)
                .delimited()
                .names("id", "timestamp", "region", "source", "level", "category", "message")
                .targetType(BattlefieldLog.class)
                .customEditors(Map.of(LocalDateTime.class, dateTimeEditor()))
                .build();
    }

    private PropertyEditor dateTimeEditor() {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDateTime.parse(text));
            }
        };
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
    public FlatFileItemWriter<BattlefieldLog> battlefieldLogFileWriter(
            @Value("#{stepExecutionContext['fileName']}") String fileName) {
        String outputFileName;

        try {
            String inputFilePath = new URL(fileName).getPath();
            outputFileName = inputFilePath + ".out";

            log.info("Thread: {} - Configuring writer for output file: {}",
                    Thread.currentThread().getName(), outputFileName);
        } catch (MalformedURLException e) {
            log.error("잘못된 입력 파일 URL 형식: file://{}", fileName, e);
            throw new IllegalArgumentException("출력 파일 경로 생성 실패: " + fileName, e);
        }

        return new FlatFileItemWriterBuilder<BattlefieldLog>()
                .name("battlefieldLogFileWriter")
                .resource(new FileSystemResource(outputFileName))
                .encoding("UTF-8")
                .delimited()
                .names("id", "timestamp", "region", "source", "level", "category", "message")
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
    public Step mergeOutputFilesStep(SystemCommandTasklet mergeFilesTasklet) {
        return new StepBuilder("mergeOutputFilesStep", jobRepository)
                .tasklet(mergeFilesTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public SystemCommandTasklet mergeFilesTasklet(@Value("#{jobParameters['path']}") String path) {
        // KILL-9: 모든 파티션 처리가 끝나면 이놈이 호출된다! 'cat' 명령으로 흩어진 '.out' 파일들을 하나로 합친다!
        SystemCommandTasklet tasklet = new SystemCommandTasklet();

        String command = String.format("cat %s/*.out > %s/%s", path, path, "merged_battlefield_logs.log");

        log.info("Executing command: {}", command);

        tasklet.setCommand("/bin/sh", "-c", command);
        tasklet.setTimeout(60000L);
        tasklet.setWorkingDirectory(path);
        tasklet.setSystemProcessExitCodeMapper(new SimpleSystemProcessExitCodeMapper());
        return tasklet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Document(collection = "battlefield_logs")
    public static class BattlefieldLog {

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
