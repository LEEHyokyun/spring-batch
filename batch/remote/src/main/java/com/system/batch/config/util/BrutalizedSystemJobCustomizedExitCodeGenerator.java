package com.system.batch.config.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class BrutalizedSystemJobCustomizedExitCodeGenerator implements JobExecutionListener, ExitCodeGenerator {
    private static int CASE1 = 1;
    private static int CASE2 = 2;
    private static int CASE3 = 3;
    private static int UNKNOWN = 4;

    private final SimpleJvmExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

    private int exitCode = 0;

    public BrutalizedSystemJobCustomizedExitCodeGenerator() {
        exitCodeMapper.setMapping(Map.of(
                "CASE1", CASE1,
                "CASE2", CASE2,
                "CASE3", CASE3,
                "UNKNOWN", UNKNOWN));
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String exitStatus = jobExecution.getExitStatus().getExitCode();

        this.exitCode = exitCodeMapper.intValue(exitStatus);
        log.info("Exit Status: {}", exitStatus);
        log.info("System Exit Code: {}", exitCode);
    }
}
