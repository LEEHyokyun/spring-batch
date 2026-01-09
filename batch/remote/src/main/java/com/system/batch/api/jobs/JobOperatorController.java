package com.system.batch.api.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@RestController
@RequestMapping("/api/operator")
@RequiredArgsConstructor
public class JobOperatorController {
    private final JobOperator jobOperator;

    private final JobExplorer jobExplorer;

    @PostMapping("/{jobName}/start")
    public ResponseEntity<String> launchJob(
            @PathVariable String jobName) throws Exception {

        Properties jobParameters = new Properties();
        jobParameters.setProperty("run.timestamp", String.valueOf(System.currentTimeMillis()));

        Long executionId = jobOperator.start(jobName, jobParameters);
        return ResponseEntity.ok("Job launched with ID: " + executionId);
    }

    @GetMapping("/{jobName}/executions")
    public ResponseEntity<List<String>> getJobExecutions(@PathVariable String jobName) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 10);
        List<String> executionInfo = new ArrayList<>();

        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
            for (JobExecution execution : executions) {
                executionInfo.add(String.format("Execution ID: %d, Status: %s",
                        execution.getId(), execution.getStatus()));
            }
        }

        return ResponseEntity.ok(executionInfo);
    }

    @PostMapping("/stop/{executionId}")
    public ResponseEntity<String> stopJob(@PathVariable Long executionId) throws Exception {
        boolean stopped = jobOperator.stop(executionId);
        return ResponseEntity.ok("Stop request for job execution " + executionId +
                (stopped ? " successful" : " failed"));
    }

    @PostMapping("/restart/{executionId}")
    public ResponseEntity<String> restartJob(@PathVariable Long executionId) throws Exception {
        Long newExecutionId = jobOperator.restart(executionId);
        return ResponseEntity.ok("Job restarted with new execution ID: " + newExecutionId);
    }
}
