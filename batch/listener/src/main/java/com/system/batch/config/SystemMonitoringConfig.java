package com.system.batch.config;

import com.system.batch.listener.BigBrotherJobExecutionListener;
import com.system.batch.listener.BigBrotherStepExecutionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class SystemMonitoringConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public SystemMonitoringConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job systemMonitoringJob(JobRepository jobRepository) {
        return new JobBuilder("systemMonitoringJob", jobRepository)
                .listener(new BigBrotherJobExecutionListener())
                .start(systemMonitoringStep())
                .build();
    }

    @Bean
    public Step systemMonitoringStep(){
        return new StepBuilder("completeQuestStep", jobRepository)
                .listener(new BigBrotherStepExecutionListener())
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("complete quest.");
                    System.out.println("Congratulations! Your Batch Step has just reached the basic one!");
                    return  RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
