package com.system.batch.config;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RecordSystemLogJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public RecordSystemLogJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job systemLogJob(Step systemLogStep) {
        return new JobBuilder("recordSystemLogJob", jobRepository)
                .start(systemLogStep)
                .build();
    }

    @Bean
    public Step systemLogStep(
            FlatFileItemReader<SystemDeath> systemLogReader,
            ItemWriter<SystemDeath> systemLogWriter
    ) {
        return new StepBuilder("recordSystemLogJob", jobRepository)
                .<SystemDeath, SystemDeath>chunk(10, transactionManager)
                .reader(systemLogReader)
                .writer(systemLogWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SystemDeath> systemLogReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<SystemDeath>()
                .name("recordSystemLogReader")
                .resource(new FileSystemResource(inputFile))
                .delimited()
                .delimiter(",")
                .names("command", "cpu", "status")
                .targetType(SystemDeath.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public ItemWriter<SystemDeath> systemLogWriter() {
        return items -> {
            for (SystemDeath item : items) {
                log.info("{}", item);
            }
        };
    }


    public static record SystemDeath(String command, int cpu, String status) {}
}