//package com.system.batch.config;
//
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
//import org.springframework.batch.core.explore.JobExplorer;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.batch.core.repository.ExecutionContextSerializer;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.boot.autoconfigure.batch.BatchDataSource;
//import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
//import org.springframework.boot.autoconfigure.batch.BatchProperties;
//import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Conditional;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.util.StringUtils;
//
//import javax.sql.DataSource;
//import java.util.concurrent.atomic.AtomicInteger;
//
//@Configuration
//@EnableConfigurationProperties(BatchProperties.class)
//public class SystemTerminationConfig extends DefaultBatchConfiguration {
//    @Override
//    protected ExecutionContextSerializer getExecutionContextSerializer() {
//        return new Jackson2ExecutionContextStringSerializer();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(prefix = "spring.batch.job", name = "enabled", havingValue = "true", matchIfMissing = true)
//    public JobLauncherApplicationRunner jobLauncherApplicationRunner(JobLauncher jobLauncher, JobExplorer jobExplorer,
//                                                                     JobRepository jobRepository, BatchProperties properties) {
//        JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(jobLauncher, jobExplorer, jobRepository);
//        String jobName = properties.getJob().getName();
//        if (StringUtils
//                .hasText(jobName)) {
//            runner.setJobName(jobName);
//        }
//        return runner;
//    }
//
//    @Configuration(proxyBeanMethods = false)
//    @Conditional(OnBatchDatasourceInitializationCondition.class)
//    static class DataSourceInitializerConfiguration {
//
//        @Bean
//        @ConditionalOnMissingBean
//        BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(DataSource dataSource,
//                                                                            @BatchDataSource ObjectProvider<DataSource> batchDataSource, BatchProperties properties) {
//            return new BatchDataSourceScriptDatabaseInitializer(batchDataSource.getIfAvailable(() -> dataSource),
//                    properties.getJdbc());
//        }
//    }
//
//    static class OnBatchDatasourceInitializationCondition extends OnDatabaseInitializationCondition {
//        OnBatchDatasourceInitializationCondition() {
//            super("Batch", "spring.batch.jdbc.initialize-schema", "spring.batch.initialize-schema");
//        }
//    }
//}
