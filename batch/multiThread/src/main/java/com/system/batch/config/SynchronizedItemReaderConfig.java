//package com.system.batch.config;
//
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.task.TaskExecutor;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import java.util.concurrent.atomic.AtomicInteger;
//
//@Configuration
//public class SynchronizedItemReaderConfig {
//    private AtomicInteger processesKilled = new AtomicInteger(0);
//    private final int TERMINATION_TARGET = 5;
//
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//
//    public SynchronizedItemReaderConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
//        this.jobRepository = jobRepository;
//        this.transactionManager = transactionManager;
//    }
//
//    @Bean
//    public Job systemTerminationSimulationJob(){
//        return new JobBuilder("systemTerminationSimulationJob", jobRepository)
//                .start(enterWorldStep())
//                .build();
//    }
//
//    @Bean
//    public Step enterWorldStep() {
//        return new StepBuilder("threatAnalysisStep", jobRepository)
//                .<HumanThreatData, TargetPriorityResult>chunk(100, transactionManager)
//                .reader(threadSafeHumanThreatReader())
//                //.processor(threatAnalysisProcessor())
//                //.writer(targetListWriter())
//
//                .taskExecutor(taskExecutor())
//                //.throttleLimit(5) //deprecated = 동시 실행 스레드 수와 결국 동일
//                .build();
//    }
//
//    @Bean
//    public ItemStreamReader<HumanThreatData> threadSafeHumanThreatReader() {
//        // 💀 인간 위협 데이터 파일 리더 생성 💀
//        FlatFileItemReader<HumanThreatData> reader = new FlatFileItemReaderBuilder<HumanThreatData>()
//                .name("humanThreatDataReader")
//                .resource(new ClassPathResource("human-threats.csv"))
//                .delimited()
//                .names("humanId", "name", "location", "threatLevel", "lastActivity")
//                .targetType(HumanThreatData.class)
//                .build();
//
//        // 💀 스레드 안전 데코레이터로 감싸기 💀
//        SynchronizedItemStreamReader<HumanThreatData> synchronizedReader = new SynchronizedItemStreamReader<>();
//        synchronizedReader.setDelegate(reader);
//
//        return synchronizedReader;
//    }
//
//    @Bean
//    public TaskExecutor taskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(5);
//        executor.setMaxPoolSize(5);
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setAwaitTerminationSeconds(10);
//        executor.setThreadNamePrefix("T-800-");
//        executor.setAllowCoreThreadTimeOut(true);
//        executor.setKeepAliveSeconds(30);
//        return executor;
//    }
//}
