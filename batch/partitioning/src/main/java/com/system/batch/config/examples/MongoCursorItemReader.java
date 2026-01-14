//package com.system.batch.config.examples;
//
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Component;
//
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//
//@Component
//public class MongoCursorItemReader{
//    @Bean
//    @StepScope
//    public MongoCursorItemReader<BattlefieldLog> mongoLogReader(
//            // Partitioner에서 ExecutionContext에
//            // Date 타입으로 시간 범위 정보를 저장했다고 가정 💀
//            @Value("#{stepExecutionContext['startDateTime']}") Date startDate,
//            @Value("#{stepExecutionContext['endDateTime']}") Date endDate) {
//        return new MongoCursorItemReaderBuilder<BattlefieldLog>()
//                .name("mongoLogReader_" + startDateTime)
//                .template(mongoTemplate)
//                .targetType(BattlefieldLog.class)
//                .collection("battlefield_logs")
//                .jsonQuery("{ 'timestamp': { '$gte': ?0, '$lt': ?1 } }")
//                .parameterValues(List.of(startDate, endDate))
//                .sorts(Collections.singletonMap("timestamp", Sort.Direction.ASC))
//                .batchSize(10000)
//                .build();
//    }
//}
//
//
