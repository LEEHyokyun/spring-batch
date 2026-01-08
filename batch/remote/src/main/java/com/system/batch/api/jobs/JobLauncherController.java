package com.system.batch.api.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobLauncherController {
    private final JobRegistry jobRegistry;

    private final JobLauncher jobLauncher;

    private final JobExplorer jobExplorer;

    @PostMapping("/{jobName}/start")
    public ResponseEntity<String> launchJob(
            @PathVariable String jobName) throws Exception {
        Job job;
        try {
            job = jobRegistry.getJob(jobName); //from jobRegistry
        } catch (NoSuchJobException e) {
            return ResponseEntity.badRequest().body("Unknown job name: " + jobName);
        }

        JobParameters jobParameters = new JobParametersBuilder(jobExplorer)
                .getNextJobParameters(job)
                .toJobParameters();

        JobExecution execution = jobLauncher.run(job, jobParameters);
        return ResponseEntity.ok("Job launched with ID: " + execution.getId());
    }
}
