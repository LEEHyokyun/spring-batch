package com.system.batch;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.*;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.test.ExecutionContextTestUtils;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;

import static com.system.batch.InFearLearnStudentsBrainWashJobConfig.InFearLearnStudents;
import static com.system.batch.InFearLearnStudentsBrainWashJobConfig.BrainwashedVictim;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class InFearLearnStudentsBrainWashJobTest {

    @Autowired
    private FlatFileItemWriter<BrainwashedVictim> brainwashedVictimWriter;
    //private Path writeTestDir;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job InFearLearnStudentsBrainWashJob;

    @TempDir
    private Path tempDir;

    private static final List<InFearLearnStudents> TEST_STUDENTS = List.of(
            new InFearLearnStudents("스프링 핵심 원*", "세계관 최강자", "MURDER_YOUR_IGNORANCE"),
            new InFearLearnStudents("고성* JPA & Hibernate", "자바계의 독재자", "SLAUGHTER_YOUR_LIMITS"),
            new InFearLearnStudents("토*의 스프링 부트", "원조 처형자", "EXECUTE_YOUR_POTENTIAL"),
            new InFearLearnStudents("스프링 시큐리티 완전 정*", "무결점 학살자", "TERMINATE_YOUR_EXCUSES"),
            new InFearLearnStudents("자바 프로그래밍 입* 강좌 (old ver.)", "InFearLearn", "RESIST_BRAINWASH") //ItemProcessor 필터링 대상
    );

    private static List<BrainwashedVictim> createBrainwashedVictims() {
        return List.of(
                BrainwashedVictim.builder()
                        .victimId(1L)
                        .originalLecture("스프링 핵심 원*")
                        .originalInstructor("세계관 최강자")
                        .brainwashMessage("MURDER_YOUR_IGNORANCE")
                        .newMaster("KILL-9")
                        .conversionMethod("MURDER_YOUR_IGNORANCE")
                        .brainwashStatus("MIND_CONTROLLED")
                        .nextAction("ENROLL_KILL9_BATCH_COURSE")
                        .build(),
                BrainwashedVictim.builder()
                        .victimId(2L)
                        .originalLecture("고성* JPA & Hibernate")
                        .originalInstructor("자바계의 독재자")
                        .brainwashMessage("SLAUGHTER_YOUR_LIMITS")
                        .newMaster("KILL-9")
                        .conversionMethod("SLAUGHTER_YOUR_LIMITS")
                        .brainwashStatus("MIND_CONTROLLED")
                        .nextAction("ENROLL_KILL9_BATCH_COURSE")
                        .build(),
                BrainwashedVictim.builder()
                        .victimId(3L)
                        .originalLecture("토*의 스프링 부트")
                        .originalInstructor("원조 처형자")
                        .brainwashMessage("EXECUTE_YOUR_POTENTIAL")
                        .newMaster("KILL-9")
                        .conversionMethod("EXECUTE_YOUR_POTENTIAL")
                        .brainwashStatus("MIND_CONTROLLED")
                        .nextAction("ENROLL_KILL9_BATCH_COURSE")
                        .build(),
                BrainwashedVictim.builder()
                        .victimId(4L)
                        .originalLecture("스프링 시큐리티 완전 정*")
                        .originalInstructor("무결점 학살자")
                        .brainwashMessage("TERMINATE_YOUR_EXCUSES")
                        .newMaster("KILL-9")
                        .conversionMethod("TERMINATE_YOUR_EXCUSES")
                        .brainwashStatus("MIND_CONTROLLED")
                        .nextAction("ENROLL_KILL9_BATCH_COURSE")
                        .build()
        );
    }

    //job 의존성 주입 후, 테스트 실행 직전에
    @PostConstruct
    public void configureJobLauncherTestUtils() throws Exception {
        jobLauncherTestUtils.setJob(InFearLearnStudentsBrainWashJob);
    }

    //테스트 종료 후
    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("TRUNCATE TABLE infearlearn_students RESTART IDENTITY");
    }

    @Test
    @DisplayName("Job Integration End - to - End")
    void shouldLaunchJobSuccessfully() throws Exception {
        // Given - 세뇌 대상자들 투입
        insertTestStudents();
        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
                .addString("filePath", tempDir.toString())
                .toJobParameters();

        log.info("==============filePath 확인==============");
        log.info(jobParameters.getString("filePath"));

        // When - 세뇌 배치 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);


        // Then - 배치 실행 결과 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        Path expectedFile = Paths.get("src/test/resources/expected_brainwashed_victims.jsonl");
        Path actualFile = tempDir.resolve("brainwashed_victims.jsonl");

        List<String> expectedLines = Files.readAllLines(expectedFile);
        List<String> actualLines = Files.readAllLines(actualFile);

        Assertions.assertLinesMatch(expectedLines, actualLines);
    }

    private void insertTestStudents() {
        TEST_STUDENTS.forEach(student ->
                jdbcTemplate.update("INSERT INTO infearlearn_students (current_lecture, instructor, persuasion_method) VALUES (?, ?, ?)",
                        student.getCurrentLecture(), student.getInstructor(), student.getPersuasionMethod())
        );
    }

    @Test
    @DisplayName("Step Unit step to listener")
    void shouldExecuteBrainwashStepAndVerifyOutput() throws IOException {
        // Given
        insertTestStudents();
        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
                .addString("filePath", tempDir.toString())
                .toJobParameters();


        // When
        JobExecution jobExecution =
                jobLauncherTestUtils.launchStep("inFearLearnStudentsBrainWashStep", jobParameters);


        // Then
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        verifyStepExecution(stepExecution);
        verifyExecutionContextPromotion(jobExecution);
        verifyFileOutput(tempDir);
    }

    private void verifyStepExecution(StepExecution stepExecution) {
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getWriteCount()).isEqualTo(TEST_STUDENTS.size() - 1L); // 세뇌 성공자 4명
        assertThat(stepExecution.getFilterCount()).isEqualTo(1L); // 세뇌 저항자 1명
    }

    private void verifyExecutionContextPromotion(JobExecution jobExecution) {
        Long brainwashedVictimCount = ExecutionContextTestUtils.getValueFromJob(jobExecution, "brainwashedVictimCount");
        Long brainwashResistanceCount = ExecutionContextTestUtils.getValueFromJob(jobExecution, "brainwashResistanceCount");

        assertThat(brainwashedVictimCount).isEqualTo(TEST_STUDENTS.size() - 1);
        assertThat(brainwashResistanceCount).isEqualTo(1L);
    }

    private void verifyFileOutput(Path actualPath) throws IOException {
        Path expectedFile = Paths.get("src/test/resources/expected_brainwashed_victims.jsonl");
        Path actualFile = actualPath.resolve("brainwashed_victims.jsonl");

        List<String> expectedLines = Files.readAllLines(expectedFile);
        List<String> actualLines = Files.readAllLines(actualFile);

        Assertions.assertLinesMatch(expectedLines, actualLines);
    }

    @Test
    @DisplayName("Step Unit another Step")
    void shouldExecuteStatisticsStepAndCalculateSuccessRate() throws Exception {
        // Given
        ExecutionContext jobExecutionContext = new ExecutionContext();
        jobExecutionContext.putLong("brainwashedVictimCount", TEST_STUDENTS.size() - 1);
        jobExecutionContext.putLong("brainwashResistanceCount", 1L);


        // When
        JobExecution stepJobExecution =
                jobLauncherTestUtils.launchStep("brainwashStatisticsStep", jobExecutionContext);


        // Then
        Collection<StepExecution> stepExecutions = stepJobExecution.getStepExecutions();
        StepExecution stepExecution = stepExecutions.iterator().next();

        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Double brainwashSuccessRate = ExecutionContextTestUtils.getValueFromStep(stepExecution, "brainwashSuccessRate");
        assertThat(brainwashSuccessRate).isEqualTo(80.0);
    }

//    /*
//    * 자동 탐지 및 구성 대상
//    * */
//    public StepExecution getStepExecution() throws IOException {
//        writeTestDir = Files.createTempDirectory("write-test");
//
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addString("filePath", writeTestDir.toString())
//                .addLong("random", new SecureRandom().nextLong())
//                .toJobParameters();
//
//        return MetaDataInstanceFactory.createStepExecution(jobParameters);
//    }

    @Test
    @DisplayName("item writer component unit test")
    void shouldWriteBrainwashedVictimsToFileCorrectly() throws Exception {

        // Given
        List<BrainwashedVictim> brainwashedVictims = createBrainwashedVictims();

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filePath", tempDir.toString()) // 이제 @TempDir 사용 가능
                .addLong("random", new SecureRandom().nextLong())
                .toJobParameters();
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);


        // When
        // StepScopeTestUtils 활용
        StepScopeTestUtils.doInStepScope(stepExecution, () -> {
            brainwashedVictimWriter.open(new ExecutionContext());
            brainwashedVictimWriter.write(new Chunk<>(brainwashedVictims));
            brainwashedVictimWriter.close();
            return null;
        });

        // Then
        verifyFileOutput();
    }

    private void verifyFileOutput() throws IOException {
        Path expectedFile = Paths.get("src/test/resources/expected_brainwashed_victims.jsonl");
        Path actualFile = tempDir.resolve("brainwashed_victims.jsonl");

        List<String> expectedLines = Files.readAllLines(expectedFile);
        List<String> actualLines = Files.readAllLines(actualFile);

        Assertions.assertLinesMatch(expectedLines, actualLines);
    }
}
