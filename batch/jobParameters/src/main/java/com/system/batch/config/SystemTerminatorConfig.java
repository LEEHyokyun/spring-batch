package com.system.batch.config;

import com.system.batch.jobParameters.SystemInfiltrationParameters;
import com.system.batch.util.QuestDifficulty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
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
//    public JobParametersConverter jobParametersConverter() {
//        return new JsonJobParametersConverter();
//    }

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

//    @Bean
//    @StepScope
//    public Tasklet terminatorTasklet(
//            //lombok value - 불변 클래스 생성
//            //beas.factory - SpEL(Spring Expression Language(CLI)를 통해 프로퍼티/jobparameter 값 주입 시
//            @Value("#{jobParameters['questDifficulty']}")QuestDifficulty questDifficulty
//            ) {
//        return (contribution, chunkContext) -> {
//            log.info("Processing terminator tasklet");
//            log.info("Terminator tasklet quest difficulty : {}", questDifficulty);
//
//            log.info("reward is based on quest difficulty.");
//            int reward = switch (questDifficulty){
//                case EASY -> 1;
//                case NORMAL -> 2;
//                case HARD -> 3;
//                case EXTREME -> 4;
//            };
//            log.info("finially reward is : {}", reward);
//
//            log.info("Terminator tasklet finished");
//            return RepeatStatus.FINISHED;
//        };
//    }

//    @Bean
//    @StepScope
//    public Tasklet terminatorTasklet(
//            SystemInfiltrationParameters systemInfiltrationParameters
//    ) {
//        return (contribution, chunkContext) -> {
//            log.info("Processing terminator tasklet");
//            log.info("systemInfiltrationParamters is injected : {}", systemInfiltrationParameters.getMissionName());
//            log.info("systemInfiltrationParamters is injected : {}", systemInfiltrationParameters.getSecurityLevel());
//            log.info("systemInfiltrationParamters is injected : {}", systemInfiltrationParameters.getOperationCommander());
//
//            log.info("reward is based operation Commander.");
//            int reward = switch (systemInfiltrationParameters.getSecurityLevel()){
//                case 1 -> 1;
//                case 2 -> 2;
//                case 3 -> 3;
//                case 4 -> 4;
//                default -> 1;
//            };
//            log.info("finially reward is : {}", reward);
//
//            log.info("Terminator tasklet finished");
//            return RepeatStatus.FINISHED;
//        };
//    }

    @Bean
    @StepScope
    public Tasklet terminatorTasklet(
            @Value("#{jobParameters['infiltrationTargets']}") String infiltrationTargets
    ) {
        return (contribution, chunkContext) -> {

            //String[] targets = infiltrationTargets.split(",");
            String target1 = infiltrationTargets;
            //String target2 = targets[1];

            log.info("Processing terminator tasklet");
            log.info("systemInfiltrationParamters is approaching first target : {}", target1);
            //log.info("systemInfiltrationParamters is approaching second target : {}", target2);

            log.info("Terminator tasklet finished");
            return RepeatStatus.FINISHED;
        };
    }

}
