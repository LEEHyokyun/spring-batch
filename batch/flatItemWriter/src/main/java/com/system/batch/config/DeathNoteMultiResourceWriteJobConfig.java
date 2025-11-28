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
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DeathNoteMultiResourceWriteJobConfig {

    @Bean
    public Job deathNoteWriteJob(
            JobRepository jobRepository,
            Step deathNoteWriteStep
    ) {
        return new JobBuilder("deathNoteMultiResourceWriteJob", jobRepository)
                .start(deathNoteWriteStep)
                .build();
    }

    @Bean
    public Step deathNoteWriteStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ListItemReader<DeathNote> deathNoteListReader,
            MultiResourceItemWriter<DeathNote> multiResourceItemWriter
    ) {
        return new StepBuilder("deathNoteWriteStep", jobRepository)
                .<DeathNote, DeathNote>chunk(10, transactionManager)
                .reader(deathNoteListReader)
                .writer(multiResourceItemWriter)
                .build();
    }

    //listItemReader
    @Bean
    public ListItemReader<DeathNote> deathNoteListReader() {
        List<DeathNote> victims = new ArrayList<>();

        for(int i = 0 ; i <= 15 ; i++){
            String id = String.format("process id : %d", i);
            LocalDate date = LocalDate.now().plusDays(i);

            victims.add(new DeathNote(
                    id,
                    "victim" + i,
                    date.format(DateTimeFormatter.ISO_DATE),
                    "case of death : " + i
            ));
        }

        return new ListItemReader<>(victims);
    }

    @Bean
    @StepScope
    public MultiResourceItemWriter<DeathNote> multiResourceItemWriter(
            @Value("#{jobParameters['outputDir']}") String outputDir
    ){
        return new MultiResourceItemWriterBuilder<DeathNote>()
                .name("multiDeathNoteWriter")
                .resource(new FileSystemResource(outputDir + "\\death_note"))  //basic name
                .itemCountLimitPerResource(10) //max line num
                .delegate(deathNoteWriter()) //위임처리
                .resourceSuffixCreator(index -> String.format("_%03d.txt", index)) //prefix
                .build();
    }

    @Bean
    public FlatFileItemWriter<DeathNote> deathNoteWriter() {
        return new FlatFileItemWriterBuilder<DeathNote>()
                .name("deathNoteWriter")
                //.resource(new FileSystemResource(outputDir + "\\death_note_report.txt"))
                .formatted()
                .format("process ID : %s | process date : %s | victim : %s | cause : %s")
                .sourceType(DeathNote.class)
                .names("victimId", "executionDate", "victimName", "causeOfDeath")
                .headerCallback(writer -> writer.write("============HEADER============"))
                .footerCallback(writer -> writer.write("============FOOTER============"))
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
