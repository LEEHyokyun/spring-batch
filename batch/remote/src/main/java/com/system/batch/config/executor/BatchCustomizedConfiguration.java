package com.system.batch.config.executor;

import org.springframework.boot.autoconfigure.batch.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class BatchCustomizedConfiguration {
    @Bean
    @BatchTaskExecutor
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor();
        return simpleAsyncTaskExecutor;
    }
}
