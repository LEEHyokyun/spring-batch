package com.system.batch.config.security.reader;

import com.system.batch.config.SecurityProtocolJobConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.system.batch.config.SecurityProtocolJobConfig.SecurityProtocol;

@Component
@Slf4j
public class SecurityProtocolReader extends AbstractItemCountingItemStreamItemReader<SecurityProtocol> {

    private List<SecurityProtocol> items = List.of(
            SecurityProtocol.builder().id(1).name("protocol 1").description("Action would be executed based on protocol 1").build(),
            SecurityProtocol.builder().id(2).name("protocol 2").description("Action would be executed based on protocol 2").build(),
            SecurityProtocol.builder().id(3).name("protocol 3").description("Action would be executed based on protocol 3").build(),
            SecurityProtocol.builder().id(4).name("protocol 4").description("Action would be executed based on protocol 4").build(),
            SecurityProtocol.builder().id(5).name("protocol 5").description("Action would be executed based on protocol 5").build(),
            SecurityProtocol.builder().id(6).name("protocol 6").description("Action would be executed based on protocol 6").build()
    );

    private int index = 0;

    public SecurityProtocolReader() {
        setName("securityProtocolReader");
    }

    @Override
    protected SecurityProtocol doRead() {
        if (index >= items.size()) {
            return null;
        }

        if (index == 0) {
            log.info("====================== [Process is Started] ======================");
            log.info("Security Protocol is Running and Detected Invasion.");
            log.info("=========================================");
            log.info("|    !!! SECURITY BREACH DETECTED !!!   |");
            log.info("|    Initializing Defense Protocol...   |");
            log.info("|    Threat Level: CRITICAL             |");
            log.info("=========================================");
            log.info("Protocol detected unusal behavior!");
            log.info("[Action Required] : JobOperator.stop() is needed for abortion.");
            log.info("[Action Required] : JobOperator needs to be aborted in 30 secconds.");

            log.info("\n====================== [COUNT DOWN] ======================");
            for (int i = 30; i > 0; i--) {
                if (i % 5 == 0) {
                    log.info("{} Seconds to be Finally Detected by whole Protocol System", i);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }

            log.info("Time is Over!");
        }

        log.info("READER IS EXECUTING ! ITEM INDEX : {}", index);

        return items.get(index++);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);

        if (executionContext.containsKey(getExecutionContextKey("started"))) {
            log.info("====================== Execution Context contains 'Started' ======================");
            log.info("Protocol Action Retry is Detected.");
            log.info("System would be Retried! \n");
        } else {
            log.info("====================== Execution Context does not contain 'Started' ======================");
            log.info("Protocol Action is Executed in normal status. \n");
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.put(getExecutionContextKey("started"), true);
        super.update(executionContext);
    }

    @Override
    protected void doOpen() throws Exception {
    }

    @Override
    protected void doClose() throws Exception {
    }

    @Override
    protected void jumpToItem(int itemIndex) {
        this.index = itemIndex;
        log.info("Jump Protocol is executed : forwared to " + (itemIndex + 1) + "!");
    }
}