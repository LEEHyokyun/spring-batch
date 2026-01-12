//package com.system.batch.config;
//
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.context.annotation.Bean;
//
//public class LectureScanJobConfig {
//    @Bean
//    public Job lectureScanConditionalJob(JobRepository jobRepository,
//                                         Step analyzeContentStep,      // State 1
//                                         Step publishLectureStep,      // State 2
//                                         Step summarizeFailureStep) {  // State 3
//
//        return new JobBuilder("lectureScanConditionalJob", jobRepository)
//                // 초기 State 설정
//                .start(analyzeContentStep)
//
//
//                // [Transition 1]
//                .on("CRITICAL")
//                .to(publishLectureStep)
//
//                // 기준점
//                .from(analyzeContentStep)
//
//                // [Transition 2]
//                .on("WARNING")
//                .to(publishLectureStep)
//
//                // [Transition 3]
//                .on("FAILURE")
//                .to(summarizeFailureStep)
//
//                // 모든 Flow 정의 종료
//                .end()
//                .build();
//    }
//}
