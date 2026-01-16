package com.system.batch.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfiguration {

    @Bean
    public NewTopic topic() {
        // 'remote-partitioning' 이라는 이름으로 토픽을 생성한다.
        return TopicBuilder.name("remote-partitioning")
                // 파티션 개수를 4개로 지정한다.
                .partitions(4)
                // replicas는 예제이므로 1로 설정 (기본값)
                .build();
    }
}
