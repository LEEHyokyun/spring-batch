package com.system.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class SystemDestructionConfig {
    @Bean
    public Job killDashNineJob(JobRepository jobRepository, Step terminationStep) {
        return new JobBuilder("killDashNineJob", jobRepository)
                .listener(systemTerminationListener(null))  // 파라미터는 런타임에 주입
                .start(terminationStep)
                .build();
    }

    @Bean
    public Step terminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("terminationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("시스템 제거 프로토콜 실행 중...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    public JobExecutionListener systemTerminationListener(
            @Value("#{jobParameters['terminationType']}") String terminationType
    ) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("시스템 제거 시작! 제거 방식: {}", terminationType);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                log.info("작전 종료! 시스템 상태: {}", jobExecution.getStatus());
            }
        };
    }
}
