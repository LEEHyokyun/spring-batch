package com.system.batch.jobParametersValidator;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

public class SystemDestructionValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        if (parameters == null)
            throw new JobParametersInvalidException("Parameters cannot be null");

        Long destructionPower = parameters.getLong("destructionPower");
        if (destructionPower == null)
            throw new JobParametersInvalidException("Parameter destructionPower cannot be null");

        if(destructionPower < 0)
            throw new JobParametersInvalidException("Parameter destructionPower cannot be negative");
    }
}
