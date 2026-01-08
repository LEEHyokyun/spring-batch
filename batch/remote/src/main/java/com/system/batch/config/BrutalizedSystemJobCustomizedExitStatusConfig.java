package com.system.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BrutalizedSystemJobCustomizedExitStatusConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job brutalizedSystemJob(Step brutalizedSystemStep, JobExecutionListener brutalizedSystemJobCustomizedExitCodeGenerator) {
        return new JobBuilder("brutalizedSystemCustomizedExitStatusJob", jobRepository)
                .start(brutalizedSystemStep)
                .listener(brutalizedSystemJobCustomizedExitCodeGenerator)
                .build();
    }

    @Bean
    public Step brutalizedSystemStep(Tasklet brutalizedSystemTasklet) {
        return new StepBuilder("brutalizedSystemStep", jobRepository)
                .tasklet(brutalizedSystemTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet brutalizedSystemTasklet(
            @Value("#{jobParameters['isFailed']}") Boolean isFailed,
            @Value("#{jobParameters['failureType']}") String failureType
    ) {
        return (contribution, chunkContext) -> {
            try {
                log.info("KILL-9 FINAL TERMINATOR :: SYSTEM INITIALIZATION");
                log.info("----------------------------------------");
                log.info("           OPERATION BRUTALIZED         ");
                log.info("----------------------------------------");
                log.info("   .-'      `-.");
                log.info("  /            \\");
                log.info(" |              |");
                log.info(" |,  .-.  .-.  ,|");
                log.info(" | )(_o/  \\o_)( |");
                log.info(" |/     /\\     \\|");
                if (isFailed) {
                    switch (failureType) {
                        case "1":
                            throw new IllegalStateException("Case 1 failure type");
                        case "2":
                            throw new ValidationException("Case 2 failure type");
                        case "3":
                            throw new IllegalArgumentException("Case 3 failure type");
                        default:
                            throw new RuntimeException("Unknown failure type");
                    }
                }
                log.info(" (_     ^^     _)");
                log.info("  \\__|IIIIII|__/");
                log.info("   | \\IIIIII/ |");
                log.info("   \\          /");
                log.info("    `--------`");
                log.info("[KILL-9 CREED PROTOCOL ACTIVATED]");
                log.info("kill9@terminator:~$ LGTM (Looks Gone To Me)");
                log.info("kill9@terminator:~$ TO FIX A BUG, KILL THE PROCESS");
                return RepeatStatus.FINISHED;
            } catch (IllegalStateException e) {
                contribution.setExitStatus(new ExitStatus("CASE1", e.getMessage()));
                throw e;
            } catch (ValidationException e){
                contribution.setExitStatus(new ExitStatus("CASE2", e.getMessage()));
                throw e;
            } catch (IllegalArgumentException e){
                contribution.setExitStatus(new ExitStatus("CASE3", e.getMessage()));
                throw e;
            } catch (RuntimeException e){
                contribution.setExitStatus(new ExitStatus("UNKNOWN", e.getMessage()));
                throw e;
            }
        };
    }
}