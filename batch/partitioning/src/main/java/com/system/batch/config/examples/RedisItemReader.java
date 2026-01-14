package com.system.batch.config.examples;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RedisItemReader{
    @Bean
    @StepScope
    public RedisItemReader<String, BattlefieldLog> redisLogReader(
            @Value("#{stepExecutionContext['startDateTime']}") LocalDateTime startDateTime) {

        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

        return new RedisItemReaderBuilder<String, BattlefieldLog>()
                .redisTemplate(redisTemplate())
                .scanOptions(ScanOptions.scanOptions()
                        // 💀 Redis에 저장된 전장 로그의 키가
                        // "logs:[날짜시간]:*" 형식으로 저장되어 있다고 가정 💀
                        .match("logs:" + startDateTime.format(FORMATTER) + ":*")
                        .count(10000)
                        .build())
                .build();
    }
}

