package com.system.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/*
 * tasklet -> batch step
 * */
@Configuration
public class ZombieBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public ZombieBatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    /*
     * chunk -> step
     * */
    @Bean
    public Step processStep() {
        /*
         * Step building by chunk (*transactionManager)
         * */
//        return new StepBuilder("processStep", jobRepository)
//                .<T1, T2>chunk(10, transactionManager)
//                .reader(itemReader())
//                .processor(itemProcessor())
//                .writer(itemWriter())
//                .build();
        return new Step() {
            @Override
            public String getName() {
                return "";
            }

            @Override
            public void execute(StepExecution stepExecution) throws JobInterruptedException {

            }
        };

    }

//    /*
//     * tasklet -> step
//     * */
//    @Bean
//    public Step zombieProcessCleanupStep() {
//        /*
//         * 트랜잭션 불필요 -> ResourcelessTransactionManager
//         * Step building by tasklet (*ResourcelessTransactionManager)
//         * */
//        return new StepBuilder("zombieCleanupStep", jobRepository)
//                .tasklet(zombieProcessCleanupTasklet(), new ResourcelessTransactionManager())
//                .build();
//    }

    @Bean
    public Job zombieProcessCleanupJob() {
        return new JobBuilder("zombieProcessCleanupJob", jobRepository)
                .start(processStep())
                .build();
    }

}