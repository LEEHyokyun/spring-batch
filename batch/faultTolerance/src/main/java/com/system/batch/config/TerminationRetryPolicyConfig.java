package com.system.batch.config;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.HttpServerErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class TerminationRetryPolicyConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job terminationRetryJob() {
        return new JobBuilder("terminationRetryPolicyJob", jobRepository)
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
                .retryPolicy(retryPolicy())
                //.retry(TerminationFailedException.class) // 재시도 대상 예외 추가
                //.retryLimit(3)
                .listener(retryListener())
                //.processorNonTransactional()
                .build();
    }

    @Bean
    public RetryPolicy retryPolicy() {
        Map<Class<? extends Throwable>, RetryPolicy> policyMap = new HashMap<>();

        SimpleRetryPolicy dbRetryPolicy = new SimpleRetryPolicy(3);
        SimpleRetryPolicy apiRetryPolicy = new SimpleRetryPolicy(5);

        policyMap.put(DataAccessException.class, dbRetryPolicy);
        policyMap.put(HttpServerErrorException.class, apiRetryPolicy);

        ExceptionClassifierRetryPolicy retryPolicy = new ExceptionClassifierRetryPolicy();
        retryPolicy.setPolicyMap(policyMap);

        return retryPolicy;
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
        return new ItemProcessor<>() {
            private static final int MAX_PATIENCE = 3;
            private int mercy = 0;  // 자비 카운트

            @Override
            public Scream process(Scream scream) throws Exception {
                System.out.print("[ItemProcessor]: check currently termination target class = " + scream);
                System.out.println(" -> Normally termination proceeded(" + scream.getProcessMsg() + ")");

                return scream;
            }
        };
    }

//    @Bean
//    public ItemWriter<Scream> terminationRetryWriter() {
//        return items -> {
//            System.out.println("[ItemWriter]: start termination retry Writer = " + items.getItems());
//
//            for (Scream scream : items) {
//                System.out.println("[ItemWriter]: writer termination is finished = " + scream);
//            }
//        };
//    }


    @Bean
    public ItemWriter<Scream> terminationRetryWriter() {
        return new ItemWriter<>() {
            private static final int MAX_PATIENCE = 3;
            private int mercy = 0;  // 자비 카운트

            @Override
            public void write(Chunk<? extends Scream> screams) {
                System.out.println("[ItemWriter]: start termination retry Writer = " + screams);

                for (Scream scream : screams) {
                    if (scream.getId() == 3 && mercy < MAX_PATIENCE) {
                        mercy ++;
                        System.out.println(" [ItemWriter]: Exception target data is detected = " + scream);
                        throw new TerminationFailedException("target data = " + scream);
                    }
                    System.out.println(" [ItemWriter]:  termination is finished, target data is = " + scream);
                }
            }
        };
    }

    @Bean
    public RetryListener retryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                System.out.println("Retry Listener is called : " + throwable + " (current retry count =" + context.getRetryCount() + "). check the retry count.\n");
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