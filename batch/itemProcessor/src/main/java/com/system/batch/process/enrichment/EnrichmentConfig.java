package com.system.batch.process.enrichment;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.mongodb.monitor.ServerInfo;

import java.time.LocalDateTime;

public class EnrichmentConfig {
    public static class SystemLog {
        private Long userId;      // 실행한 사용자
        private String rawCommand;  // 원본 명령어
        private LocalDateTime executedAt; // 실행 시간

        // API 호출로 보강될 필드들
        private String serverName;  // 서버 정보
        private String processName; // 프로세스 정보
        private String riskLevel;   // 위험 등급
    }

    @Slf4j
    @RequiredArgsConstructor
    public class SystemLogEnrichItemProcessor implements ItemProcessor<SystemLog, SystemLog> {
        private final ObservabilityApiClient observabilityApiClient;
        //private final HttpClient httpClient


        @Override
        public SystemLog process(SystemLog systemLog) {
            // 입력: SystemLog{userId=666, rawCommand='kill -9 1234', executedAt=2025-01-15T10:30:00, serverName=null, processName=null, riskLevel=null}

            // 외부 API 호출해서 서버 정보 보강 💀
            ServerInfo serverInfo = observabilityApiClient.getServerInfo(systemLog.getUserId());

            // 기존 SystemLog 객체에 보강된 정보 추가 💀
            systemLog.setServerName(serverInfo.getHostName());
            systemLog.setProcessName(serverInfo.getCurrentProcess());
            systemLog.setRiskLevel(calculateRiskLevel(serverInfo, systemLog.getRawCommand()));

            // 출력: SystemLog{userId=666, rawCommand='kill -9 1234', executedAt=2025-01-15T10:30:00, serverName='chaos-api-05', processName='system-reaper', riskLevel='HIGH'}
            return systemLog;
        }
    }
}
