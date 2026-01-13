//package com.system.batch.config;
//
//import com.system.batch.decider.ReviewDecider;
//import com.system.batch.flow.LectureValidationFlow;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.StepExecution;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.batch.core.job.builder.FlowBuilder;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.job.flow.Flow;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.core.step.tasklet.Tasklet;
//import org.springframework.batch.item.ExecutionContext;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class LectureReviewJobFlowConfig {
//
//    //private final LectureValidationFlow lectureValidationFlow;
//    private final Flow lectureValidationFlow;
//
//    @Bean
//    public Job newCourseReviewJob(JobRepository jobRepository,
//                                  Flow lectureValidationFlow,
//                                  Step notifyInstructorStep) {
//        return new JobBuilder("newCourseReviewJob", jobRepository)
//                .start(notifyInstructorStep)    // Flow 완료 후 추가 Step 실행
//                .on("FLOW_STEP").to(lectureValidationFlow)
//                .end()
//                .build();
//    }
//
////    @Bean
////    public Job validationStep(JobRepository jobRepository,
////                                  Flow lectureValidationFlow,
////                                  Step notifyInstructorStep) {
////        return new StepBuilder("newCourseReviewJob", jobRepository)
////                .flow(lectureValidationFlow)
////                .end()
////                .build();
////    }
//
//}
