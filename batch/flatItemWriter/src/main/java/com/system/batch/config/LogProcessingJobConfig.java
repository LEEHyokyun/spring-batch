package com.system.batch.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class LogProcessingJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public LogProcessingJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job logProcessingJob(
            //class type 일치 = 빈일치
            Step createDirectoryStep,
            Step logCollectionStep,
            Step logProcessingStep
    ) {
        return new JobBuilder("logProcessingJob", jobRepository)
                .start(createDirectoryStep) //directory
                .next(logCollectionStep)    //log collection
                .next(logProcessingStep)    //log processor
                .build();
    }

    //class type 일치 = 빈일치
    //"변수명/이름"이 우선순위가 아닌 "type"이 우선순위
    //변수명/이름이 같아도 type이 다르면, 다른 type을 마음대로 교체해버린다.
    @Bean
    public Step createDirectoryStep(Tasklet mkdirTasklet) {
        return new StepBuilder("createDirectoryStep", jobRepository)
                .tasklet(mkdirTasklet, transactionManager)
                .build();
    }

//    //-p 옵션은 윈도우에서 안먹힘..따라서 collected_logs라는 디렉토리 생성(mkdir)은 처음부터 존재하지 않아야 한다.
//    //SystemCommandTasklet -> 반드시 cli 명령어 작성 필요
//    @Bean
//    @StepScope
//    public SystemCommadTasklet mkdirTasklet(
//            @Value("#{jobParameters['date']}") String date) throws IOException {
//        SystemCommandTasklet tasklet = new SystemCommandTasklet();
////        //tasklet.setWorkingDirectory(System.getProperty("user.home"));
//        //실행환경은 batch 명령어 작성환경에 상관없이 무조건 cmd
//        //경로직접주임은 반드시 백슬래쉬
//        tasklet.setWorkingDirectory("C:\\Users\\gyrbs\\OneDrive\\Desktop");
//
//        String collectedLogsPath = "collected_logs\\" + date;
//        String processedLogsPath = "processed_logs\\" + date;
//        //String collectedLogsPath = String.format("collected_logs\\%s", date);
//        //String processedLogsPath = String.format("processed_logs\\%s", date);
//        //String command = String.format("mkdir %s && mkdir %s", collectedLogsPath, processedLogsPath);
////
//        //tasklet.setCommand("mkdir", "-p", collectedLogsPath, processedLogsPath);
//        //tasklet.setCommand("cmd", "/c", "mkdir", collectedLogsPath);
//        tasklet.setCommand("cmd", "/c", command);
//
//
//        tasklet.setTimeout(3000); // 3초 타임아웃
//        return tasklet;
//    }

    //-p 옵션은 윈도우에서 안먹힘..따라서 collected_logs라는 디렉토리 생성(mkdir)은 처음부터 존재하지 않아야 한다.
    //tasklet -> return (con, ch)
    @Bean
    @StepScope
    public Tasklet mkdirTasklet(
            @Value("#{jobParameters['date']}") String date) throws IOException {
        return ((contribution, chunkContext) -> {

            Path desktop = Paths.get("C:\\Users\\gyrbs\\OneDrive\\Desktop");

            Path collected = desktop.resolve("collected_logs").resolve(date);
            Path processed = desktop.resolve("processed_logs").resolve(date);

            Files.createDirectories(collected);
            Files.createDirectories(processed);

            return RepeatStatus.FINISHED;
        });
    }

    @Bean
    public Step logCollectionStep(SystemCommandTasklet scpTasklet) {
        return new StepBuilder("logCollectionStep", jobRepository)
                .tasklet(scpTasklet, transactionManager)
                .build();
    }

    //class type 일치 = 빈일치
    @Bean
    @StepScope
    public SystemCommandTasklet scpTasklet(
            @Value("#{jobParameters['date']}") String date) {
        SystemCommandTasklet tasklet = new SystemCommandTasklet();
        //tasklet.setWorkingDirectory(System.getProperty("user.home"));
        String rootDirectory = "C:\\Users\\gyrbs\\OneDrive\\Desktop";
        tasklet.setWorkingDirectory(rootDirectory);

        String collectedLogsPath = String.format("collected_logs\\%s", date);

        StringJoiner commandBuilder = new StringJoiner(" && ");
        for (String host : List.of("localhost", "loan", "pay")) {
            //String command = String.format("scp %s:~\\logs\\%s.log .%s\\%s.log",
            //        host, date, processedLogsPath, host);
            String src = String.format("logs\\%s.log", date);
            String dest = String.format("%s\\%s.log", collectedLogsPath, host);

            commandBuilder.add(String.format("copy %s %s", src, dest));
        }

        //String src = String.format("logs\\%s.log", date);
        //String dest = String.format("%s\\%s.log", collectedLogsPath, "localhost");

        tasklet.setCommand("cmd", "/c", commandBuilder.toString());
        tasklet.setTimeout(10000); //10초 타임아웃


        return tasklet;
    }

    @Bean
    public Step logProcessingStep(
            MultiResourceItemReader<LogEntry> multiResourceItemReader,
            LogEntryProcessor logEntryProcessor,
            FlatFileItemWriter<ProcessedLogEntry> processedLogEntryJsonWriter
    ) {
        return new StepBuilder("logProcessingStep", jobRepository)
                .<LogEntry, ProcessedLogEntry>chunk(10, transactionManager)
                .reader(multiResourceItemReader)
                .processor(logEntryProcessor)
                .writer(processedLogEntryJsonWriter)
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<LogEntry> multiResourceItemReader(
            @Value("#{jobParameters['date']}") String date) {
        MultiResourceItemReader<LogEntry> resourceItemReader = new MultiResourceItemReader<>();
        resourceItemReader.setName("multiResourceItemReader");
        resourceItemReader.setResources(getResources(date));

        resourceItemReader.setDelegate(logFileReader());
        return resourceItemReader;
    }

    private Resource[] getResources(String date) {
        try {
            //String userHome = System.getProperty("user.home");
            String userHome = "C:/Users/gyrbs/OneDrive/Desktop";
            String location = "file:" + userHome + "/collected_logs/" + date + "/*.log";

            //경로 직접 주입이 아닌 Resolver를 통한 url구성은 백슬래쉬가 아닌 /로 해야 인식
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            return resolver.getResources(location);
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve log files", e);
        }
    }

    @Bean
    public FlatFileItemReader<LogEntry> logFileReader() {
        return new FlatFileItemReaderBuilder<LogEntry>()
                .name("logFileReader")
                .delimited()
                .delimiter(",")
                .names("dateTime", "level", "message")
                .targetType(LogEntry.class)
                .build();
    }

    @Bean
    public LogEntryProcessor logEntryProcessor() {
        return new LogEntryProcessor();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<ProcessedLogEntry> processedLogEntryJsonWriter(
            @Value("#{jobParameters['date']}") String date) {
        String userHome = "C:\\Users\\gyrbs\\OneDrive\\Desktop";
        //String userHome = System.getProperty("user.home");
        String outputPath = Paths.get(userHome, "processed_logs", date, "processed_logs.jsonl").toString();

        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);

        return new FlatFileItemWriterBuilder<ProcessedLogEntry>()
                .name("processedLogEntryJsonWriter")
                .resource(new FileSystemResource(outputPath))
                .lineAggregator(item -> {
                    try {
                        return objectMapper.writeValueAsString(item);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error converting item to JSON", e);
                    }
                })
                .build();
    }

    @Data
    public static class LogEntry {
        private String dateTime;
        private String level;
        private String message;
    }

    @Data
    public static class ProcessedLogEntry {
        private LocalDateTime dateTime;
        private LogLevel level;
        private String message;
        private String errorCode;
    }

    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG, UNKNOWN;

        public static LogLevel fromString(String level) {
            if (level == null || level.trim().isEmpty()) {
                return UNKNOWN;
            }
            try {
                return valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }

    //inner class
    public static class LogEntryProcessor implements ItemProcessor<LogEntry, ProcessedLogEntry> {
        private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
        private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("ERROR_CODE\\[(\\w+)]");

        @Override
        public ProcessedLogEntry process(LogEntry item) {
            ProcessedLogEntry processedEntry = new ProcessedLogEntry();
            processedEntry.setDateTime(parseDateTime(item.getDateTime()));
            processedEntry.setLevel(parseLevel(item.getLevel()));
            processedEntry.setMessage(item.getMessage());
            processedEntry.setErrorCode(extractErrorCode(item.getMessage()));
            return processedEntry;
        }

        private LocalDateTime parseDateTime(String dateTime) {
            return LocalDateTime.parse(dateTime, ISO_FORMATTER);
        }

        private LogLevel parseLevel(String level) {
            return LogLevel.fromString(level);
        }

        private String extractErrorCode(String message) {
            if (message == null) {
                return null;
            }

            Matcher matcher = ERROR_CODE_PATTERN.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
            // ERROR 문자열이 포함되어 있지만 패턴이 일치하지 않는 경우
            if (message.contains("ERROR")) {
                return "UNKNOWN_ERROR";
            }
            return null;
        }
    }
}