package com.system.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BrutalizedSystemJobRESTAPIConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job brutalizedSystemJob() {
        return new JobBuilder("brutalizedSystemRESTAPIJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(brutalizedSystemStep())
                .build();
    }

    @Bean
    public Step brutalizedSystemStep() {
        return new StepBuilder("brutalizedSystemStep", jobRepository)
                .tasklet(brutalizedSystemTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet brutalizedSystemTasklet() {
        return (contribution, chunkContext) -> {
            log.info("KILL-9 FINAL TERMINATOR :: SYSTEM INITIALIZATION");
            log.info("----------------------------------------");
            log.info("|          OPERATION BRUTALIZED         |");
            log.info("----------------------------------------");
            log.info("______________________");
            log.info("   .-'      `-.");
            log.info("  /            \\");
            log.info(" |              |");
            log.info(" |,  .-.  .-.  ,|");
            log.info(" | )(_o/  \\o_)( |");
            log.info(" |/     /\\     \\|");
            log.info(" (_     ^^     _)");
            log.info("  \\__|IIIIII|__/");
            log.info("   | \\IIIIII/ |");
            log.info("   \\          /");
            log.info("    `--------`");
            log.info("[KILL-9 CREED PROTOCOL ACTIVATED]");
            log.info("kill9@terminator:~$ LGTM (Looks Gone To Me)");
            log.info("kill9@terminator:~$ TO FIX A BUG, KILL THE PROCESS");
            return RepeatStatus.FINISHED;
        };
    }
}

