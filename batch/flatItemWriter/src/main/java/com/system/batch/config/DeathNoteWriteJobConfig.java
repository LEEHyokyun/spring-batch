package com.system.batch.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
public class DeathNoteWriteJobConfig {

    @Bean
    public Job deathNoteWriteJob(
            JobRepository jobRepository,
            Step deathNoteWriteStep
    ) {
        return new JobBuilder("deathNoteWriteJob", jobRepository)
                .start(deathNoteWriteStep)
                .build();
    }

    @Bean
    public Step deathNoteWriteStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ListItemReader<DeathNote> deathNoteListReader,
            FlatFileItemWriter<DeathNote> deathNoteWriter
    ) {
        return new StepBuilder("deathNoteWriteStep", jobRepository)
                .<DeathNote, DeathNote>chunk(10, transactionManager)
                .reader(deathNoteListReader)
                .writer(deathNoteWriter)
                .build();
    }

    //listItemReader
    @Bean
    public ListItemReader<DeathNote> deathNoteListReader() {
        List<DeathNote> victims = List.of(
                new DeathNote(
                        "KILL-001",
                        "001",
                        "2025-11-26",
                        "CPU OVERFLOW"),
                new DeathNote(
                        "KILL-002",
                        "002",
                        "2025-11-26",
                        "JVM STACK OVERFLOW"),
                new DeathNote(
                        "KILL-003",
                        "003",
                        "2025-11-26",
                        "NO HEAP MEMORY MORE")
        );

        return new ListItemReader<>(victims);
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<DeathNote> deathNoteWriter(
            @Value("#{jobParameters['outputDir']}") String outputDir) {
        return new FlatFileItemWriterBuilder<DeathNote>()
                .name("deathNoteWriter")
                .resource(new FileSystemResource(outputDir + "\\death_notes.csv"))
                .delimited()
                .delimiter(",")
                .sourceType(DeathNote.class)
                .names("victimId", "victimName", "executionDate", "causeOfDeath")
                .headerCallback(writer -> writer.write("처형ID,피해자명,처형일자,사인"))
                .build();
    }

    @Data
    @AllArgsConstructor
    public static class DeathNote {
        private String victimId;
        private String victimName;
        private String executionDate;
        private String causeOfDeath;
    }
}
