/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.listener;

import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import static org.apache.servicecomb.saga.common.EventType.SagaStartedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

/**
 * @author Gannalyo
 * @since 2019-08-31
 */
public class TxEventAfterPersistingListener implements Observer {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private ITxleCache txleCache;

    @Autowired
    private IKafkaMessageProducer kafkaMessageProducer;

    @Autowired
    private IDataDictionaryService dataDictionaryService;

    private final Set<String> serverNameIdCategory = new HashSet<>();

    @Override
    public void update(Observable arg0, Object arg1) {
        if (arg0 != null) {
            // TODO 监控也放在此处
            TxEvent event = ((GlobalTxListener) arg0).getEvent();
            if (event != null) {
                log.info("The listener [{}] observes the new event [" + event.toString() + "].", this.getClass());
                String type = event.type();
                if (SagaStartedEvent.name().equals(type)) {
                    // 当有新的全局事务时，设置最小id查询次数加1，即需要查询最小事件id
                    EventScanner.UNENDED_MIN_EVENT_ID_SELECT_COUNT.incrementAndGet();
                    this.putServerNameIdCategory(event);
                } else if (TxStartedEvent.name().equals(type)) {
                    this.putServerNameIdCategory(event);
                } else if (EventType.SagaEndedEvent.name().equals(event.type())) {
                    // TODO batch calling
                    // TODO 新起的server先从其他leader同步所有缓存
                    txleCache.removeDistributedTxSuspendStatusCache(event.globalTxId());
                    txleCache.removeDistributedTxAbortStatusCache(event.globalTxId());
                    kafkaMessageProducer.send(event);
                }
            }
        }
    }

    private void putServerNameIdCategory(TxEvent event) {
        final String globalTxServer = "global-tx-server-info";
        final String serverNameInstanceCategory = event.serviceName() + "__" + event.instanceId() + "__" + event.category();
        boolean result = serverNameIdCategory.add(serverNameInstanceCategory);
        if (result) {
            new Thread(() -> {
                int showOrder = dataDictionaryService.selectMaxShowOrder(globalTxServer);
                final DataDictionaryItem ddItem = new DataDictionaryItem(globalTxServer, event.serviceName(), event.instanceId(), event.category(), showOrder + 1, 1, "");
                dataDictionaryService.createDataDictionary(ddItem);
            }).start();
        }
    }

}
