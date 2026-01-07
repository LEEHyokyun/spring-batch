package com.system.batch.config;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.batch.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
public class SystemTerminationRefactoringConfig extends DefaultBatchConfiguration {
    @Bean
    protected ExecutionContextSerializer getExecutionContextSerializer() {
        return new Jackson2ExecutionContextStringSerializer();
    }

    // 비즈니스 데이터 처리를 위한 메인 데이터 소스 🏴‍☠
    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/target_system")
                .username("root")
                .password("1q2w3e")
                .build();
    }

    // Spring Batch 메타데이터 저장을 위한 데이터 소스
    @Bean
    @BatchDataSource
    public DataSource batchDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/batch_control")
                .username("kill9")
                .password("d3str0y3r")
                .build();
    }

    // 비즈니스 데이터 처리용 JPA 트랜잭션 매니저
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // Spring Batch 메타데이터용 트랜잭션 매니저
    @Bean
    @BatchTransactionManager
    public PlatformTransactionManager batchTransactionManager(@BatchDataSource DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}
