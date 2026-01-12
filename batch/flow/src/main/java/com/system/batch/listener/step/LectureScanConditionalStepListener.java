package com.system.batch.listener.step;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class LectureScanConditionalStepListener implements StepExecutionListener {
    private static final int CRITICAL_SKIP_THRESHOLD = 10;
    private static final int WARNING_SKIP_THRESHOLD = 5;

    @Override
    public void beforeStep(StepExecution stepExecution) {

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long skipCount = stepExecution.getSkipCount();

        if (skipCount >= CRITICAL_SKIP_THRESHOLD) {
            return new ExitStatus("CRITICAL",
                    String.format("CRITICAL STEP IS NEEDED : %d", skipCount));
        } else if (skipCount >= WARNING_SKIP_THRESHOLD) {
            return new ExitStatus("WARNING",
                    String.format("WARNING STEP IS NEEDED : %d", skipCount));
        } else if (skipCount > 0) {
            return new ExitStatus("FAILURE",
                    String.format("STEP IS FAILED : %d", skipCount));
        }

        // 기존 ExitStatus 유지
        return stepExecution.getExitStatus();
    }
}
