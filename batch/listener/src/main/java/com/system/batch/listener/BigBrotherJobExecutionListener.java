package com.system.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BigBrotherJobExecutionListener implements JobExecutionListener {
//    @Override
//    public void beforeJob(JobExecution jobExecution){
//      log.info("This is BigBrotherJobExecutionListener.beforeJob");
//    }

      @BeforeJob
      public void beforeJob(){
        log.info("This is BigBrotherJobExecutionListener.beforeJob");
      }

//    @Override
//    public void afterJob(JobExecution jobExecution){
//        log.info("This is BigBrotherJobExecutionListener.afterJob");
//        log.info("Check the jobExecution status : {}", jobExecution.getStatus());
//    }

      @AfterJob
      public void afterJob(){
          log.info("This is BigBrotherJobExecutionListener.afterJob");
      }
}
