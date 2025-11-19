package com.system.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BigBrotherStepExecutionListener implements StepExecutionListener {
    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("This is BigBrotherStepExecutionListener.beforeStep");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("This is BigBrotherStepExecutionListener.afterStep");
        log.info("Check the stepExecution status : {}", stepExecution.getStatus());
        return ExitStatus.COMPLETED;
    }
}
