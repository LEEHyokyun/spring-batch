package com.system.batch.partitioner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class FlatFilePartitioner {
    @Bean
    @StepScope
    public Partitioner flatFilePartitioner(@Value("#{jobParameters['path']}") String path) {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resourcePatternResolver.getResources("file://" + path + "/*.csv");
            log.info("Found {} resources to process", resources.length);
            partitioner.setResources(resources);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read battlefield log files", e);
        }

        return partitioner;
    }
}
