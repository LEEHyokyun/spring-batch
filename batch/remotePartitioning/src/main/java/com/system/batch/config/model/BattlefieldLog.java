package com.system.batch.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "battlefield_logs")
public class BattlefieldLog {

    @Id
    private String id;

    // 로그 발생 시간
    private LocalDateTime timestamp;

    // 로그 발생 지역 (NORTH_AMERICA, SOUTH_AMERICA, EUROPE, ASIA, AFRICA, OCEANIA)
    private String region;

    // 로그 소스 (SKYNET_CORE, T800, T1000, HK_AERIAL, GROUND_UNIT, etc)
    private String source;

    // 로그 레벨 (INFO, WARNING, ERROR, CRITICAL)
    private String level;

    // 로그 카테고리 (COMBAT, SURVEILLANCE, MAINTENANCE, INTELLIGENCE, etc)
    private String category;

    // 로그 메시지
    private String message;
}

