package com.system.batch.process.listener;

import com.system.batch.config.SystemLogProcessingValidatorConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.data.mongodb.monitor.ServerInfo;

import java.util.List;
import java.util.Map;

import static com.system.batch.config.SystemLogProcessingValidatorConfig.SystemLog;

@Slf4j
@RequiredArgsConstructor
public class SystemLogEnrichListener implements ItemWriteListener<SystemLog> {
    private final ObservabilityApiClient observabilityApiClient;

    @Override
    public void beforeWrite(Chunk<? extends SystemLog> items) {
        List<Long> userIds = items.getItems().stream()
                .map(SystemLog::getUserId)
                .toList();

        // 벌크 API 호출: 청크 단위로 서버 정보를 한 번에 조회 💀
        Map<Long, ServerInfo> serverInfoMap = observabilityApiClient.fetchServerInfos(userIds);

        // 조회된 정보로 각 SystemLog 보강 💀
        items.getItems().forEach(systemLog -> {
            ServerInfo serverInfo = serverInfoMap.get(systemLog.getUserId());
            if (serverInfo != null) {
//                systemLog.setServerName(serverInfo.getHostName());
//                systemLog.setProcessName(serverInfo.getCurrentProcess());
//                systemLog.setRiskLevel(calculateRiskLevel(serverInfo, systemLog.getRawCommand()));
            }
        });

        log.info("SystemLog data enrichment is finished", items.size());
    }
}
