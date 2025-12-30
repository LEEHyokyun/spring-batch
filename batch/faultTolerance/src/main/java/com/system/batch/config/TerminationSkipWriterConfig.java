package com.system.batch.config;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TerminationSkipWriterConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job terminationRetryJob() {
        return new JobBuilder("terminationSkipWriterJob", jobRepository)
                .start(terminationRetryStep())
                .build();
    }

    @Bean
    public Step terminationRetryStep() {
        return new StepBuilder("terminationRetryStep", jobRepository)
                .<Scream, Scream>chunk(3, transactionManager)
                .reader(terminationRetryReader())
                .processor(terminationRetryProcessor())
                .writer(terminationRetryWriter())
                .faultTolerant() // 내결함성 기능 ON
                .skip(TerminationFailedException.class)
                .skipLimit(2)
                //.skipPolicy(new AlwaysSkipItemSkipPolicy())
                .processorNonTransactional()
                .build();
    }

    @Bean
    public ListItemReader<Scream> terminationRetryReader() {
        return new ListItemReader<>(List.of(
                Scream.builder()
                        .id(1)
                        .scream("scream : id : 1")
                        .processMsg("processMsg : id : 1")
                        .build(),
                Scream.builder()
                        .id(2)
                        .scream("scream : id : 2")
                        .processMsg("processMsg : id : 2")
                        .build(),
                Scream.builder()
                        .id(3)
                        .scream("scream : id : 3")
                        .processMsg("processMsg : id : 3")
                        .build(),
                Scream.builder()
                        .id(4)
                        .scream("scream : id : 4")
                        .processMsg("processMsg : id : 4")
                        .build(),
                Scream.builder()
                        .id(5)
                        .scream("scream : id : 5")
                        .processMsg("processMsg : id : 5")
                        .build(),
                Scream.builder()
                        .id(6)
                        .scream("System.exit(-666)")
                        .processMsg("processMsg : id : 6 : rm -rf kill -9")
                        .build()
        )) {
            @Override
            public Scream read() {
                Scream scream = super.read();
                if(scream == null) {
                    return null;
                }
                System.out.println("[ItemReader]: check currently termiation target = " + scream);
                return scream;
            }
        };
    }

    @Bean
    public ItemProcessor<Scream, Scream> terminationRetryProcessor() {
        return scream -> {
            System.out.println(" [ItemProcessor]: to be processed = " + scream);
            return scream;
        };
    }

    @Bean
    public ItemWriter<Scream> terminationRetryWriter() {
        return items -> {
            System.out.println("[ItemWriter]: item Writer is now on going : " + items.getItems());

            for (Scream scream : items) {
                if(scream.getId() == 4 || scream.getId() == 5) {
                    System.out.println("[ItemWriter]: Exception would be throwed : " + scream);
                    throw new TerminationFailedException("Target : " + scream);
                }
                System.out.println("[ItemWriter]: Writer is normally finished : " + scream);
            }
        };
    }

    public static class TerminationFailedException extends RuntimeException {
        public TerminationFailedException(String message) {
            super(message);
        }
    }

    @Getter
    @Builder
    public static class Scream {
        private int id;
        private String scream;
        private String processMsg;

        @Override
        public String toString() {
            return id + "_" + scream;
        }
    }
}