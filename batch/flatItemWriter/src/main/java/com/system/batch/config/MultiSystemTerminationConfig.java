package com.system.batch.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class MultiSystemTerminationConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public MultiSystemTerminationConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job systemFailureJob(Step systemFailureStep) {
        return new JobBuilder("multiSystemFailureJob", jobRepository)
                .start(systemFailureStep)
                .build();
    }

    @Bean
    public Step systemFailureStep(
            MultiResourceItemReader<SystemFailure> multiSystemFailureItemReader,
            SystemFailureStdoutItemWriter systemFailureStdoutItemWriter
    ) throws Exception {
        return new StepBuilder("multiSystemFailureStep", jobRepository)
                .<SystemFailure, SystemFailure>chunk(10, transactionManager)
                .reader(multiSystemFailureItemReader)
                .writer(systemFailureStdoutItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<SystemFailure> multiSystemFailureItemReader(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath) {
        return new MultiResourceItemReaderBuilder<SystemFailure>()
                .name("multiSystemFailureItemReader")
                .resources(new Resource[]{
                        //읽기 = 알파벳 순, 내부적인 comparator로 customizing
                        new FileSystemResource(inputFilePath + "\\" +"critical-failures.csv"),
                        new FileSystemResource(inputFilePath + "\\" +"normal-failures.csv")
                })
                .delegate(systemFailureItemReader())
                .build();
    }

    @Bean
    public FlatFileItemReader<SystemFailure> systemFailureItemReader() {
        return new FlatFileItemReaderBuilder<SystemFailure>()
                .name("multiSystemFailureItemReader")
                .delimited()
                .delimiter(",")
                .names("errorId",
                        "errorDateTime",
                        "severity",
                        "processId",
                        "errorMessage")
                .targetType(SystemFailure.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public SystemFailureStdoutItemWriter systemFailureStdoutItemWriter() {
        return new SystemFailureStdoutItemWriter();
    }

    public static class SystemFailureStdoutItemWriter implements ItemWriter<SystemFailure> {
        @Override
        public void write(Chunk<? extends SystemFailure> chunk) throws Exception {
            for (SystemFailure failure : chunk) {
                log.info("Processing system failure: {}", failure);
            }
        }
    }

    @Data
    public static class SystemFailure {
        private String errorId;
        private String errorDateTime;
        private String severity;
        private Integer processId;
        private String errorMessage;
    }
}
