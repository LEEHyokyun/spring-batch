package com.system.batch.config;

import com.system.batch.util.QuestDifficulty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class SystemTerminatorConfig {
    @Bean
    public Job processTerminatorJob(JobRepository jobRepository, Step terminationStep) {
        return new JobBuilder("processTerminatorJob", jobRepository)
                .start(terminationStep)
                .build();
    }

    @Bean
    public Step terminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet terminatorTasklet) {
        return new StepBuilder("terminationStep", jobRepository)
                .tasklet(terminatorTasklet, transactionManager)
                .build();
    }

//    @Bean
//    @StepScope
//    public Tasklet terminatorTasklet(
//            //lombok value - 불변 클래스 생성
//            //beas.factory - SpEL(Spring Expression Language(CLI)를 통해 프로퍼티/jobparameter 값 주입 시
//            @Value("#{jobParameters['terminatorId']}") String terminatorId,
//            @Value("#{jobParameters['targetCount']}") Integer targetCount
//    ) {
//        return (contribution, chunkContext) -> {
//          log.info("Processing terminator tasklet");
//          log.info("Terminator tasklet id : {}", terminatorId);
//          log.info("Terminator tasklet targetCount : {}", targetCount);
//
//          for(int i = 0 ; i <= targetCount ; i++){
//              log.info("target terminating System is Running : {}/{}", i, targetCount);
//          }
//
//          log.info("Terminator tasklet finished");
//          return RepeatStatus.FINISHED;
//        };
//    }

    @Bean
    @StepScope
    public Tasklet terminatorTasklet(
            //lombok value - 불변 클래스 생성
            //beas.factory - SpEL(Spring Expression Language(CLI)를 통해 프로퍼티/jobparameter 값 주입 시
            @Value("#{jobParameters['questDifficulty']}")QuestDifficulty questDifficulty
            ) {
        return (contribution, chunkContext) -> {
            log.info("Processing terminator tasklet");
            log.info("Terminator tasklet quest difficulty : {}", questDifficulty);

            log.info("reward is based on quest difficulty.");
            int reward = switch (questDifficulty){
                case EASY -> 1;
                case NORMAL -> 2;
                case HARD -> 3;
                case EXTREME -> 4;
            };
            log.info("finially reward is : {}", reward);

            log.info("Terminator tasklet finished");
            return RepeatStatus.FINISHED;
        };
    }
}
