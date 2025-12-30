//package com.system.batch.config;
//
//import lombok.Builder;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
//import org.springframework.batch.item.Chunk;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.batch.item.ItemWriter;
//import org.springframework.batch.item.support.ListItemReader;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.dao.DataAccessException;
//import org.springframework.retry.RetryCallback;
//import org.springframework.retry.RetryContext;
//import org.springframework.retry.RetryListener;
//import org.springframework.retry.RetryPolicy;
//import org.springframework.retry.backoff.ExponentialBackOffPolicy;
//import org.springframework.retry.backoff.FixedBackOffPolicy;
//import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
//import org.springframework.retry.policy.SimpleRetryPolicy;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.web.client.HttpServerErrorException;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Configuration
//@RequiredArgsConstructor
//public class TerminationSkipConfig {
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//
//    @Bean
//    public Job terminationRetryJob() {
//        return new JobBuilder("terminationSkipJob", jobRepository)
//                .start(terminationRetryStep())
//                .build();
//    }
//
//    @Bean
//    public Step terminationRetryStep() {
//        return new StepBuilder("terminationRetryStep", jobRepository)
//                .<Scream, Scream>chunk(3, transactionManager)
//                .reader(terminationRetryReader())
//                .processor(terminationRetryProcessor())
//                .writer(terminationRetryWriter())
//                .faultTolerant() // 내결함성 기능 ON
//                .skip(TerminationFailedException.class)
//                .skipLimit(1)
//                //.skipPolicy(new AlwaysSkipItemSkipPolicy())
//                .processorNonTransactional()
//                .build();
//    }
//
//    @Bean
//    public ListItemReader<Scream> terminationRetryReader() {
//        return new ListItemReader<>(List.of(
//                Scream.builder()
//                        .id(1)
//                        .scream("scream : id : 1")
//                        .processMsg("processMsg : id : 1")
//                        .build(),
//                Scream.builder()
//                        .id(2)
//                        .scream("scream : id : 2")
//                        .processMsg("processMsg : id : 2")
//                        .build(),
//                Scream.builder()
//                        .id(3)
//                        .scream("scream : id : 3")
//                        .processMsg("processMsg : id : 3")
//                        .build(),
//                Scream.builder()
//                        .id(4)
//                        .scream("scream : id : 4")
//                        .processMsg("processMsg : id : 4")
//                        .build(),
//                Scream.builder()
//                        .id(5)
//                        .scream("scream : id : 5")
//                        .processMsg("processMsg : id : 5")
//                        .build(),
//                Scream.builder()
//                        .id(6)
//                        .scream("System.exit(-666)")
//                        .processMsg("processMsg : id : 6 : rm -rf kill -9")
//                        .build()
//        )) {
//            @Override
//            public Scream read() {
//                Scream scream = super.read();
//                if(scream == null) {
//                    return null;
//                }
//                System.out.println("[ItemReader]: check currently termiation target = " + scream);
//                return scream;
//            }
//        };
//    }
//
//    @Bean
//    public ItemProcessor<Scream, Scream> terminationRetryProcessor() {
//        return scream -> {
//            System.out.print(" [ItemProcessor]: to be processed = " + scream);
//
//            if (scream.getId() == 2 || scream.getId() == 5) {
//                System.out.println(" -> TerminationFailedException would be throwed : ");
//                throw new TerminationFailedException("Exception throwed by = " + scream);
//            } else {
//                System.out.println(" -> Normally Terminated(" + scream.getProcessMsg() + ")");
//            }
//
//            return scream;
//        };
//    }
//
//    @Bean
//    public ItemWriter<Scream> terminationRetryWriter() {
//        return items -> {
//            System.out.println("[ItemWriter]: itme Writer is now on going : " + items.getItems());
//
//            for (Scream scream : items) {
//                System.out.println("[ItemWriter]: Writer is normally finised : " + scream);
//            }
//        };
//    }
//
//    public static class TerminationFailedException extends RuntimeException {
//        public TerminationFailedException(String message) {
//            super(message);
//        }
//    }
//
//    @Getter
//    @Builder
//    public static class Scream {
//        private int id;
//        private String scream;
//        private String processMsg;
//
//        @Override
//        public String toString() {
//            return id + "_" + scream;
//        }
//    }
//}