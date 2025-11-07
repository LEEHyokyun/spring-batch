package com.system.batch.init;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class SystemTerminationConfig {
    private AtomicInteger processesKilled = new AtomicInteger(0);
    private final int TERMINATION_TARGET = 5;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public SystemTerminationConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job systemTerminationSimulationJob(){
        return new JobBuilder("systemTerminationSimulationJob", jobRepository)
                .start(enterWorldStep())
                .next(meetNPCStep())
                .next(defeatProcessStep())
                .next(completeQuestStep())
                .build();
    }

    @Bean
    public Step enterWorldStep(){
        return new StepBuilder("enterWorldStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Entered to System Termination World.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step meetNPCStep(){
        return new StepBuilder("meetNPCStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("meet NPC.");
                    System.out.println("First mission : Zombie process : " + TERMINATION_TARGET + " has been Killed");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step defeatProcessStep(){
        return new StepBuilder("defeatProcessStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int terminated = processesKilled.incrementAndGet();
                    System.out.println("defeatProcessStep is Running : target has been killed by "+ terminated);

                    if(terminated < TERMINATION_TARGET){
                        return RepeatStatus.CONTINUABLE;
                    }else{
                        return RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }

    @Bean
    public Step completeQuestStep(){
        return new StepBuilder("completeQuestStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("complete quest.");
                    System.out.println("Congratulations! Your Batch Step has just reached the basic one!");
                    return  RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
