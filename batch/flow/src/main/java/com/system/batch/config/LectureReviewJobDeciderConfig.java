package com.system.batch.config;

import com.system.batch.decider.ReviewDecider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Configuration
public class LectureReviewJobDeciderConfig {

    @Bean
    public Job lectureReviewJobDecider(JobRepository jobRepository,
                                ReviewDecider reviewDecider,
                                Step analyzeLectureStep,
                                Step approveImmediatelyStep,
                                Step initiateContainmentProtocolStep,
                                Step lowQualityRejectionStep,
                                Step priceGougerPunishmentStep,
                                Step adminManualCheckStep) {
        return new JobBuilder("lectureReviewJobDecider", jobRepository)
                .start(analyzeLectureStep) // 모든 것은 강의 분석에서 시작된다...
                .next(reviewDecider)
                .on("666_SPRING_BATCH").to(approveImmediatelyStep)

                .from(reviewDecider) // 다시 분석 스텝으로 돌아와서...
                .on("EXCELLENT_COURSE").to(initiateContainmentProtocolStep)  //  표절 의심? 즉시 격리 및 저작권 위반 심문 시작 💀

                .from(reviewDecider) //  또 다시 분석 스텝...
                .on("EXCELLENT_COURSE").to(priceGougerPunishmentStep)      // 수강생 등골 브레이커 탐지! '바가지 요금 처단' 스텝으로 보내 경제 정의 실현!

                .from(reviewDecider) //  마지막이다...
                .on("AVERAGE_COURSE").to(adminManualCheckStep)     // 💀💀💀💀 컨텐츠 담당자 공포에 떨며 검토 중 💀💀💀💀

                .from(reviewDecider)
                .on("NEEDS_IMPROVEMENT").to(lowQualityRejectionStep)

                .end() // Flow 종료
                .build();
    }


    @Bean
    public Step analyzeLectureStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager) {
        return new StepBuilder("analyzeLectureStep", jobRepository)
                .tasklet(analyzeLectureTasklet(lectureList()), transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step approveImmediatelyStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("approveImmediatelyStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("강의 승인 처리 완료! 인프런에 즉시 게시되었습니다.");
                    log.info("강사에게 승인 메일 발송됨.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step initiateContainmentProtocolStep(JobRepository jobRepository,
                                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("initiateContainmentProtocolStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.warn("표절 의심 강의 격리 프로토콜 가동!");
                    log.warn("강의 비공개 처리 및 접근 제한 설정 완료.");
                    log.warn("표절 검증팀에 검토 요청 발송됨.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step lowQualityRejectionStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("lowQualityRejectionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.warn("퀄리티 미달 강의 반려 처리!");
                    log.warn("강사에게 품질 개선 가이드 발송.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step priceGougerPunishmentStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager) {
        return new StepBuilder("priceGougerPunishmentStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.warn("바가지 요금 강의 징계 처리!");
                    log.warn("7일 이내 가격 조정 필요 안내.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step adminManualCheckStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("adminManualCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.error("미확인 위험 강의 관리자 긴급 검토 요청!");
                    log.error("보안팀 및 콘텐츠 담당자에게 긴급 알림 발송.");
                    log.error("컨텐츠 담당자 수동 검토 후 조치 예정.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet analyzeLectureTasklet(List<Lecture> lectureList) {
        return (contribution, chunkContext) -> {
            // 랜덤하게 강의 선택
            Random random = new Random();
            int reviewScore = random.nextInt(12);

            log.info("Average review score: {}", reviewScore);

            StepExecution stepExecution = contribution.getStepExecution();
            ExecutionContext executionContext = stepExecution.getExecutionContext();
            executionContext.putInt("reviewScore", reviewScore);

            return RepeatStatus.FINISHED;
        };
    }

    public record Lecture(String title, int price, String content) { }

    @Bean
    public List<Lecture> lectureList() {
        List<Lecture> lectures = new ArrayList<>();

        // 정상 강의
        lectures.add(new Lecture(
                "스프링 배치 완벽 안내서",
                29900,
                """
                        ...
                        1장: 지루한 배치 프로세싱 개요 /
                        2장: 누구나 아는 스프링 배치 아키텍처 /"
                        3장: 졸리게 설명하는 JobRepository와 메타데이터 /
                        4장: 뻔한 청크지향처리 설명 /
                        5장: 특별할 것 없는 에러 핸들링 /
                        6장: 흔한 스프링 배치 확장과 최적화 /
                        7장: 교과서적인 배치 테스트 /
                        강의를 들어도 실력이 늘지 않는다면 환불은 불가합니다.
                        ...
                        """
        ));

        // 고가 강의
        lectures.add(new Lecture(
                "1주일만에 억대 연봉 개발자 되기",
                99000,
                """
                        ...
                        수강생 99%가 증명한 월 천만원 자동화 비법
                        부의 추월차선 탑승! 당신도 할 수 있습니다
                        실리콘밸리 탑티어가 압축한 성공 로드맵
                        아무도 알려주지 않는 연봉 퀀텀 점프 성공 공식
                        네카라쿠배 현직자의 특급 노하우
                        ...
                        """
        ));

        // 표절 의심 강의
        lectures.add(new Lecture(
                "Copy & Paste로 배우는 자바스크립트",
                39900,
                "... [여기에 내용 붙여넣기 - 출처: fake-it-till-you-make-it.dev/spring-batch-flow/34] ..."
        ));

        // 내용 부실 강의
        lectures.add(new Lecture(
                "5분 완성 Spring Batch 핵심 개념",
                13200,
                "... ChatGPT는 실수를 할 수 있습니다. ..."
        ));

        // 시스템 패닉 강의 by KILL9
        lectures.add(new Lecture(
                "죽음의 Spring Batch",
                66666,
                """
                        ...
                        서버의 마지막 숨결까지 쥐어짜는 법. CPU 100%는 준비운동에 불과하다 ☠️
                        데이터 무결성 따위는 잊어라. 지금 당장 멀쩡해 보이면 된다. 몇 달 뒤 원인 불명의 대재앙을 선사하라! ☠️
                        시스템 곳곳에 심어두는 논리 시한폭탄 컬렉션. 우리팀의 워라밸은 나의 손에 달렸다 ☠️
                        로그? 그건 겁쟁이들이나 남기는 것. ☠️
                        에러 처리는 사치. 모든 예외는 catch(Exception ignored) { } ☠️
                        주석 없는 1만 라인 Tasklet 실습. 당신의 사수는 당신 몰래 이직 준비를 시작할 것이다. ☠️
                        ...
                        """
        ));

        return lectures;
    }
}
