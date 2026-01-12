//package com.system.batch.config;
//
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.context.annotation.Bean;
//
//public class LectureReviewJobConfig {
//    @Bean
//    public Job lectureReviewJob(JobRepository jobRepository,
//                                Step analyzeLectureStep,
//                                Step approveImmediatelyStep,
//                                Step initiateContainmentProtocolStep,
//                                Step lowQualityRejectionStep,
//                                Step priceGougerPunishmentStep,
//                                Step adminManualCheckStep) {
//        return new JobBuilder("inflearnLectureReviewJob", jobRepository)
//                .start(analyzeLectureStep) // start
//                .on("CASE1").to(approveImmediatelyStep)
//
//                .from(analyzeLectureStep) // from - on - to
//                .on("CASE2").to(initiateContainmentProtocolStep)
//
//                .from(analyzeLectureStep) //  from - on - to
//                .on("CASE3").to(priceGougerPunishmentStep)
//
//                .from(analyzeLectureStep) //  from - on - to
//                .on("CASE4").to(adminManualCheckStep)
//
//                .end() // Flow 종료
//                .build();
//    }
//}
