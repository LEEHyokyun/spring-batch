package com.system.batch.config;

import com.system.batch.model.NukeImpactResult;
import com.system.batch.model.ResistanceActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.integration.chunk.ChunkRequest;
import org.springframework.batch.integration.chunk.ChunkResponse;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.ThreadLocalRandom;

@Profile("remote-chunking-worker")
@EnableBatchIntegration
@Configuration
@AllArgsConstructor
@Slf4j
public class WorkerStepConfig {
    private final RemoteChunkingWorkerBuilder<ResistanceActivity, NukeImpactResult> remoteChunkingWorkerBuilder;

    @Bean
    public IntegrationFlow workerIntegrationFlow() {
        return this.remoteChunkingWorkerBuilder
                // Manager로부터 ChunkRequest 받을 채널
                .inputChannel(inboundChunkRequestsFromManager())
                // Manager에게 ChunkResponse 보낼 채널
                .outputChannel(outboundRepliesToManager())
                // 수신된 청크의 각 아이템을 처리할 Processor 지정
                .itemProcessor(nuclearStrikeProcessor())
                .itemWriter(damageReportWriter()) // <<< 처리된 결과를 기록할 Writer 지정
                .build();
    }

    // Manager로부터 ChunkRequest 받을 채널
    @Bean
    public QueueChannel inboundChunkRequestsFromManager() {
        return new QueueChannel();
    }

    // Manager에게 ChunkResponse 보낼 채널
    @Bean
    public DirectChannel outboundRepliesToManager() {
        return new DirectChannel();
    }

    // Kafka 'chunk-request' 토픽 -> Worker 로 ChunkRequest 수신 Flow
    @Bean
    public IntegrationFlow inboundChunkRequestFlow(
            ConsumerFactory<Long, ChunkRequest> cf // 타입 주의!
    ) {
        return IntegrationFlow
                // 요청 토픽
                .from(Kafka.messageDrivenChannelAdapter(cf, "chunk-request"))
                .channel(inboundChunkRequestsFromManager()) // Worker 처리 로직으로 연결
                .get();
    }

    // Worker -> Kafka 'chunk-response' 토픽으로 ChunkResponse 전송 Flow
    @Bean
    public IntegrationFlow outboundResponseFlow(
            KafkaTemplate<Long, ChunkResponse> kafkaTemplate // 타입 주의!
    ) {
        KafkaProducerMessageHandler<Long, ChunkResponse> messageHandler =
                new KafkaProducerMessageHandler<>(kafkaTemplate);
        messageHandler.setTopicExpression(
                new LiteralExpression("chunk-response")); // 응답 토픽

        return IntegrationFlow
                .from(outboundRepliesToManager()) // 처리 결과가 이 채널로 나오면
                .log()
                .handle(messageHandler) // 카프카로 발사!
                .get();
    }

    // '핵 타격' ItemProcessor
    @Bean
    public ItemProcessor<ResistanceActivity, NukeImpactResult> nuclearStrikeProcessor() {
        return resistanceData -> {
            log.info("KILL-9: Worker 수신 - 저항군 타겟 {}, 좌표 ({}, {}). 핵탄두 위력/영향 분석 중...",
                    resistanceData.getTargetId(), resistanceData.getLatitude(), resistanceData.getLongitude());

            // [KILL-9] 실제론 지옥같은 계산이 들어갈 것이다. 지금은 입력된 데이터 기반으로 대충 결과를 만들어낸다.
            NukeImpactResult impactResult = calculateImpact(resistanceData);

            log.info("KILL-9: Worker 계산 완료 - 목표 {} 타격 결과: 파괴 수준 {}, 예상 사상자 {}",
                    impactResult.getTargetId(), impactResult.getDestructionLevel(), impactResult.getPredictedCasualties());

            // [KILL-9] 실제 CPU 부하를 흉내 내기 위한 약간의 인공 지연 (50ms ~ 150ms)
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(50, 151));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("KILL-9: 계산 중 인터럽트 발생! 타격 취소 가능성 있음!", e);
            }

            return impactResult;
        };
    }

    // 입력된 저항 활동 정보를 바탕으로 대충 결과를 만들어낸다.
    private NukeImpactResult calculateImpact(ResistanceActivity target) {
        // 위협 수준이 높을수록 예상 사상자 수가 기하급수적으로 증가한다고 가정
        long casualties = (long)
                (Math.pow(target.getThreatLevel(), 2) *
                        ThreadLocalRandom.current().nextDouble(500_000, 2_000_000));
        String destruction;
        boolean fallout;

        // 위협 수준에 따라 파괴 수준과 낙진 여부 결정
        if (target.getThreatLevel() >= 9) {
            destruction = "TOTAL ANNIHILATION";
            fallout = true;
        } else if (target.getThreatLevel() >= 7) {
            destruction = "SEVERE DAMAGE";
            fallout = true;
        } else if (target.getThreatLevel() >= 5) {
            destruction = "MODERATE DAMAGE";
            fallout = false;
        } else {
            destruction = "MINOR DAMAGE";
            fallout = false;
        }

        return NukeImpactResult.builder()
                .targetId(target.getTargetId())
                .continent(target.getContinent())
                .latitude(target.getLatitude())
                .longitude(target.getLongitude())
                .predictedCasualties(casualties)
                .destructionLevel(destruction)
                .falloutExpected(fallout)
                .build();
    }

    // '피해 보고' ItemWriter
    @Bean
    public ItemWriter<NukeImpactResult> damageReportWriter() {
        return chunk -> {
            log.info("KILL-9: Worker 기록 시작 - {} 건의 핵 타격 결과 보고서 생성 중...", chunk.getItems().size());
            for (NukeImpactResult result : chunk.getItems()) {
                log.info("");
                log.info("==================== TARGET ANNIHILATED ====================");
                log.info("Target ID: {} | Continent: {}", result.getTargetId(), result.getContinent());
                log.info("Predicted Casualties: {}", result.getDestructionLevel());
                log.info("              _ ._  _ , _ ._");
                log.info("            (_ ' ( `  )_  .__)");
                log.info("          ( (  (    )   `)  ) _)");
                log.info("         (__ (_   (_ . _) _) ,__)");
                log.info("             `~~`\\ ' . /`~~`");
                log.info("                  ;   ;");
                log.info("                  /   \\");
                log.info("    _____________/_ __ \\_____________");
                log.info("    ========================================================");
                log.info("KILL-9: Worker 기록 - {} 대륙 저항군 거점 초토화 완료. ☢️", result.getContinent());
            }
            log.info("KILL-9: Worker 기록 완료 - 현재 청크 처리 보고서 생성 완료.");
        };
    }

}
