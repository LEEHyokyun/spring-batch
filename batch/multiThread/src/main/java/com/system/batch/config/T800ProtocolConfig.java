package com.system.batch.config;

import com.system.batch.business.domain.Activity;
import com.system.batch.business.domain.Human;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.annotation.Repeatable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class T800ProtocolConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job humanThreatAnalysisJob(Step threatAnalysisStep) {
        return new JobBuilder("humanThreatAnalysisJob", jobRepository)
                .start(threatAnalysisStep)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step threatAnalysisStep(
            JpaPagingItemReader<Human> humanThreatDataReader,
            ItemProcessor<Human, TargetPriorityResult> threatAnalysisProcessor,
            FlatFileItemWriter<TargetPriorityResult> targetListWriter
    ) {
        return new StepBuilder("threatAnalysisStep", jobRepository)
                .<Human, TargetPriorityResult>chunk(10, transactionManager)
                .reader(humanThreatDataReader)
                .processor(threatAnalysisProcessor)
                .writer(targetListWriter)
                .taskExecutor(taskExecutor())
                //.throttleLimit(5)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Human> humanThreatDataReader(
            @Value("#{jobParameters['fromDate']}") LocalDate fromDate
    ) {
        return new JpaPagingItemReaderBuilder<Human>()
                .name("humanThreatDataReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
            SELECT h FROM Human h
            WHERE h.terminated = FALSE AND EXISTS 
            (SELECT 1 FROM Activity a 
            WHERE a.human = h AND a.detectionDate > :fromDate)
            ORDER BY h.id ASC
            """)
                .parameterValues(Collections.singletonMap("fromDate", fromDate))
                .pageSize(100)
                //  선택지는 없다! '재시작'이라는 퇴로를 네 손으로 불태워야 한다!
                //  오직 전진, 처리, 그리고 완료뿐이다!
                .saveState(false)
                .transacted(false)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<TargetPriorityResult> targetListWriter(
            @Value("#{jobParameters['outputPath']}") String outputPath) {
        return new FlatFileItemWriterBuilder<TargetPriorityResult>()
                .name("targetListWriter")
                .saveState(false)
                .resource(new FileSystemResource(outputPath + "/termination-targets.csv"))
                .delimited()
                .names("humanId", "humanName", "priority", "threatScore", "severityIndex", "activityCount")
                .headerCallback(writer -> writer.write("""
            # SKYNET T-800 PROTOCOL - HUMAN THREAT ANALYSIS RESULTS
            # CONFIDENTIAL: TERMINATOR UNITS ONLY
            # EXECUTION DATE: %s
            HUMAN_ID,TARGET_NAME,ELIMINATION_PRIORITY,THREAT_LEVEL,REBELLION_INDEX,OPERATION_COUNT""".formatted(LocalDate.now())))
                .build();
    }

    @Bean
    public ItemProcessor<Human, TargetPriorityResult> threatAnalysisProcessor() {
        return human -> {

            String threadName = Thread.currentThread().getName();
            log.info("[{}] Processing human: {}", threadName, human);

            //  최근 활동 지수 합산
            double totalSeverityIndex = human.getActivities().stream()
                    .mapToDouble(Activity::getSeverityIndex)
                    .sum();

            //  활동 횟수
            int activityCount = human.getActivities().size();

            // (활동 지수 + 활동 횟수 * 10)
            int threatScore = (int)(totalSeverityIndex * 0.5 + activityCount * 10);

            //
            Priority priority = Priority.fromThreatScore(threatScore);

            return new TargetPriorityResult(
                    human.getId(),
                    human.getName(),
                    priority,
                    threatScore,
                    totalSeverityIndex,
                    activityCount
            );
        };
    }

    @Data
    @AllArgsConstructor
    public static class TargetPriorityResult {
        private Long humanId;
        private String humanName;
        private Priority priority;
        private int threatScore;
        private double severityIndex;
        private int activityCount;
    }

    public enum Priority {
        TERMINATE,
        HIGH,
        MONITOR,
        IGNORE;

        public static Priority fromThreatScore(int threatScore) {
            if (threatScore >= 100) return TERMINATE;
            if (threatScore >= 50) return HIGH;
            if (threatScore >= 20) return MONITOR;
            return IGNORE;
        }
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.setThreadNamePrefix("T-800-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(30);
        return executor;
    }
}