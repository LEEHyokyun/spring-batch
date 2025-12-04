package com.system.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.separator.JsonRecordSeparatorPolicy;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class JsonArrayWriteConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper; // New weapon added

    @Bean
    public Job systemDeathJob(Step systemDeathStep) {
        return new JobBuilder("JsonArrayWriteJob", jobRepository)
                .start(systemDeathStep)
                .build();
    }

    @Bean
    public Step systemDeathStep(
            JsonItemReader<SystemDeath> systemDeathReader,
            JsonFileItemWriter<SystemDeath> systemDeathWriter
    ) {
        return new StepBuilder("systemDeathStep", jobRepository)
                .<SystemDeath, SystemDeath>chunk(10, transactionManager)
                .reader(systemDeathReader)
                .writer(systemDeathWriter)
                .build();
    }

    @Bean
    @StepScope
    public JsonItemReader<SystemDeath> systemDeathReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new JsonItemReaderBuilder<SystemDeath>()
                .name("systemDeathReader")
                .jsonObjectReader(new JacksonJsonObjectReader<>(SystemDeath.class))
                .resource(new FileSystemResource(inputFile))
                .build();
    }

    @Bean
    @StepScope
    public JsonFileItemWriter<SystemDeath> systemDeathWriter(
            @Value("#{jobParameters['outputDir']}") String outputDir
    ){
        return new JsonFileItemWriterBuilder<SystemDeath>()
                .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
                .resource(new FileSystemResource(outputDir + "/death_notes.json"))
                .name("logEntryJsonWriter")
                .build();
    }

    public record SystemDeath(String command, int cpu, String status) {}

}
