//package com.system.batch.flow;
//
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.FlowBuilder;
//import org.springframework.batch.core.job.flow.Flow;
//import org.springframework.context.annotation.Bean;
//import org.springframework.stereotype.Component;
//
//
//@Component
//public class LectureValidationFlow {
//    @Bean
//    public Flow lectureValidationFlow() {
//        return new FlowBuilder<Flow>("lectureValidationFlow")
//                .start(validateContentStep)
//                .next(checkPlagiarismStep)
//                .on("PLAGIARISM_DETECTED").fail()  // 표절 감지되면 즉시 실패 처리
//                .from(checkPlagiarismStep)
//                .on("COMPLETED").to(verifyPricingStep)
//                .on("TOO_EXPENSIVE").to(pricingWarningStep)  // 가격이 과도하면 경고 처리
//                .from(verifyPricingStep)
//                .on("*").end()  // 나머지 모든 경우는 정상 종료
//                .build();
//    }
//}
