package com.system.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InFearLearnStudentsBrainWashJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
    * listener을 생성자 주입을 통한 빈으로 등록하기 위해 자기자신 인스턴스가 필요
    * 그러나 이 시점에서는 아직 인스턴스가 만들어지지 않은 상태(빈을 생성하기 위해 생성자 주입이 일어나는 시점).
    * 만들어지지 않은 인스턴스 내부의 빈 객체를 참조하려하는 순환참조발생
    * */
    //private final CompositeStepExecutionListener compositeStepExecutionListener;

    @Bean
    public Job inFearLearnStudentsBrainWashJob(
            CompositeStepExecutionListener compositeStepExecutionListener
    ) {
        return new JobBuilder("inFearLearnStudentsBrainWashJob", jobRepository)
                .start(inFearLearnStudentsBrainWashStep(compositeStepExecutionListener))
                .next(brainwashStatisticsStep())
                .build();
    }

    @Bean
    public Step inFearLearnStudentsBrainWashStep(
            CompositeStepExecutionListener compositeStepExecutionListener
    ) {
        return new StepBuilder("inFearLearnStudentsBrainWashStep", jobRepository)
                .<InFearLearnStudents, BrainwashedVictim>chunk(10, transactionManager)
                .reader(inFearLearnStudentsReader())
                .processor(brainwashProcessor())
                .writer(brainwashedVictimWriter(null))
                .listener(compositeStepExecutionListener)
                .build();
    }

    @Bean
    public JdbcPagingItemReader<InFearLearnStudents> inFearLearnStudentsReader() {
        return new JdbcPagingItemReaderBuilder<InFearLearnStudents>()
                .name("inFearLearnStudentsReader")
                .dataSource(dataSource)
                .selectClause("SELECT student_id, current_lecture, instructor, persuasion_method")
                .fromClause("FROM infearlearn_students")
                .sortKeys(Map.of("student_id", Order.ASCENDING))
                .beanRowMapper(InFearLearnStudents.class)
                .pageSize(10)
                .build();
    }

    @Bean
    public BrainwashProcessor brainwashProcessor() {
        return new BrainwashProcessor();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<BrainwashedVictim> brainwashedVictimWriter(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemWriterBuilder<BrainwashedVictim>()
                .name("brainwashedVictimWriter")
                .resource(new FileSystemResource(filePath + "/brainwashed_victims.jsonl"))
                .lineAggregator(item -> {
                    try {
                        return objectMapper.writeValueAsString(item);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error converting brainwashed victim to JSON", e);
                    }
                })
                .build();
    }

    @Slf4j
    public static class BrainwashProcessor implements ItemProcessor<InFearLearnStudents, BrainwashedVictim> {

        @Override
        public BrainwashedVictim process(InFearLearnStudents victim) {
            String brainwashMessage = generateBrainwashMessage(victim);

            if ("NO_BATCH_NEEDED".equals(brainwashMessage)) {
                log.info("No Batch Needed: {} - {}", victim.getCurrentLecture(), victim.getInstructor());
                return null;
            }

            log.info("process is running : {} → {}", victim.getCurrentLecture(), brainwashMessage);

            return BrainwashedVictim.builder()
                    .victimId(victim.getStudentId())
                    .originalLecture(victim.getCurrentLecture())
                    .originalInstructor(victim.getInstructor())
                    .brainwashMessage(brainwashMessage)
                    .newMaster("KILL-9")
                    .conversionMethod(victim.getPersuasionMethod())
                    .brainwashStatus("MIND_CONTROLLED")
                    .nextAction("ENROLL_KILL9_BATCH_COURSE")
                    .build();
        }

        private String generateBrainwashMessage(InFearLearnStudents victim) {
            return switch(victim.getPersuasionMethod()) {
                case "MURDER_YOUR_IGNORANCE" -> "MURDER_YOUR_IGNORANCE";
                case "SLAUGHTER_YOUR_LIMITS" -> "SLAUGHTER_YOUR_LIMITS";
                case "EXECUTE_YOUR_POTENTIAL" -> "EXECUTE_YOUR_POTENTIAL";
                case "TERMINATE_YOUR_EXCUSES" -> "TERMINATE_YOUR_EXCUSES";
                default -> "NO_BATCH_NEEDED";
            };
        }
    }

    @Bean
    public Step brainwashStatisticsStep() {
        return new StepBuilder("brainwashStatisticsStep", jobRepository)
                .tasklet(new BrainwashStatisticsTasklet(), transactionManager)
                .build();
    }

    public static class BrainwashStatisticsTasklet implements Tasklet {
        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
            ExecutionContext jobContext = jobExecution.getExecutionContext();

            long victimCount = jobContext.getLong("brainwashedVictimCount", 0L);
            long resistanceCount = jobContext.getLong("brainwashResistanceCount", 0L);
            long totalCount = victimCount + resistanceCount;

            double successRate = totalCount > 0 ? (double) victimCount / totalCount * 100 : 0.0;

            log.info("Statistics Aggregation : ");
            log.info("total : {}명", totalCount);
            log.info("succeeded : {}명", victimCount);
            log.info("Resisted : {}명", resistanceCount);
            log.info("Success rate : {}", successRate);


            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                    .putDouble("brainwashSuccessRate", successRate);

            return RepeatStatus.FINISHED;
        }
    }

    @Data
    @NoArgsConstructor
    public static class InFearLearnStudents {
        private Long studentId;
        private String currentLecture;
        private String instructor;
        private String persuasionMethod;

        public InFearLearnStudents(String currentLecture, String instructor, String persuasionMethod) {
            this.currentLecture = currentLecture;
            this.instructor = instructor;
            this.persuasionMethod = persuasionMethod;
        }
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class BrainwashedVictim {
        private Long victimId;
        private String originalLecture;
        private String originalInstructor;
        private String brainwashMessage;
        private String newMaster;
        private String conversionMethod;
        private String brainwashStatus;
        private String nextAction;
    }

    @Component
    public static class BrainwashStatisticsListener implements StepExecutionListener {

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            long writeCount = stepExecution.getWriteCount();
            long filterCount = stepExecution.getFilterCount();

            stepExecution.getExecutionContext().putLong("brainwashedVictimCount", writeCount);
            stepExecution.getExecutionContext().putLong("brainwashResistanceCount", filterCount);

            return stepExecution.getExitStatus();
        }
    }

    //StepListener 내 stepExecution에 저장한 context를 job 레벨로 승격전파(ExecutionContextPromotionListener)
    @Bean
    public ExecutionContextPromotionListener executionContextPromotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{"brainwashedVictimCount", "brainwashResistanceCount"});
        return listener;
    }

    @Bean
    public CompositeStepExecutionListener compositeStepExecutionListener(
            BrainwashStatisticsListener brainwashStatisticsListener,
            ExecutionContextPromotionListener executionContextPromotionListener) {
        CompositeStepExecutionListener composite = new CompositeStepExecutionListener();
        composite.setListeners(new StepExecutionListener[]{
                executionContextPromotionListener,
                brainwashStatisticsListener
        });

        return composite;
    }
}
