/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.listener;

import com.actionsky.txle.cache.CacheName;
import com.actionsky.txle.cache.ITxleEhCache;
import com.actionsky.txle.grpc.interfaces.ICustomRepository;
import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxleMetrics;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.servicecomb.saga.common.EventType.SagaStartedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

/**
 * @author Gannalyo
 * @since 2019-08-30
 */
public class TxEventAfterPersistingListener implements Observer {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private ITxleCache txleCache;

    @Autowired
    private IKafkaMessageProducer kafkaMessageProducer;

    @Autowired
    private IDataDictionaryService dataDictionaryService;

    @Autowired
    private ICustomRepository customRepository;

    @Autowired
    private ITxleEhCache txleEhCache;

    @Autowired
    private TxleMetrics txleMetrics;

    private final Set<String> serverNameIdCategory = new HashSet<>();

    private final Set<String> globalTxIdSet = new HashSet<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    public void update(Observable arg0, Object arg1) {
        if (arg0 != null) {
            TxEvent event = ((GlobalTxListener) arg0).getEvent();
            if (event != null) {
                if (event.id() == -1) {
                    txleMetrics.startMarkTxDuration(event);
                    txleMetrics.countTxNumber(event);
                } else {
                    txleMetrics.endMarkTxDuration(event);

                    log.info("The listener [{}] observes the new event [" + event.toString() + "].", this.getClass());
                    switch (EventType.valueOf(event.type())) {
                        case SagaStartedEvent:
                            // increase 1 for the minimum identify of undone event when some global transaction starts.
                            EventScanner.UNENDED_MIN_EVENT_ID_SELECT_COUNT.incrementAndGet();
                            this.putServerNameIdCategory(event);
                            break;
                        case TxStartedEvent:
                            this.putServerNameIdCategory(event);
                            break;
                        case SagaEndedEvent:
                            globalTxIdSet.add(event.globalTxId());
                            kafkaMessageProducer.send(event);

                            // 1M = 1024 * 1024 = 1048576, 1048576 / 36 = 29172
                            if (globalTxIdSet.size() > 20000) {
                                removeDistributedTxStatusCache(globalTxIdSet);
                            }

                            this.saveBusinessDBBackupInfo(event);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void saveBusinessDBBackupInfo(TxEvent event) {
        Object cacheValue = txleEhCache.get(CacheName.GLOBALTX, "backup-table-check");
        if (cacheValue != null) {
            Map<String, List<String[]>> values = (Map<String, List<String[]>>) cacheValue;
            if (values != null && !values.isEmpty()) {
                List<String[]> backupInfoList = values.get(event.globalTxId());
                if (backupInfoList != null) {
                    Set<String> backupTables = new HashSet<>();
                    backupInfoList.forEach(backupInfo -> {
                        if (!backupTables.contains(backupInfo[2])) {
                            if (!this.checkIsExistsBackupTable(event.serviceName(), event.instanceId(), backupInfo[0], backupInfo[1], backupInfo[2])) {
                                if (this.customRepository.executeUpdate("INSERT INTO BusinessDBBackupInfo (servicename, instanceid, dbnodeid, dbschema, backuptablename) VALUES (?, ?, ?, ?, ?)", event.serviceName(), event.instanceId(), backupInfo[0], backupInfo[1], backupInfo[2]) > 0) {
                                    backupTables.add(backupInfo[2]);
                                }
                            } else {
                                backupTables.add(backupInfo[2]);
                            }
                        }
                        if (!backupTables.contains(backupInfo[3])) {
                            if (!this.checkIsExistsBackupTable(event.serviceName(), event.instanceId(), backupInfo[0], backupInfo[1], backupInfo[3])) {
                                if (this.customRepository.executeUpdate("INSERT INTO BusinessDBBackupInfo (servicename, instanceid, dbnodeid, dbschema, backuptablename) VALUES (?, ?, ?, ?, ?)", event.serviceName(), event.instanceId(), backupInfo[0], backupInfo[1], backupInfo[3]) > 0) {
                                    backupTables.add(backupInfo[3]);
                                }
                            } else {
                                backupTables.add(backupInfo[3]);
                            }
                        }
                    });
                    values.remove(event.globalTxId());
                    txleEhCache.put(CacheName.GLOBALTX, "backup-table-check", values);
                }
            }
        }
    }

    private boolean checkIsExistsBackupTable(String serviceName, String instanceId, String dbNodeId, String database, String backupTableName) {
        String sql = "SELECT COUNT(1) FROM BusinessDBBackupInfo T WHERE T.servicename = ? AND T.instanceid = ? AND T.dbnodeid = ? AND T.dbschema = ? AND T.backuptablename = ? AND T.status = ?";
        return this.customRepository.count(sql, serviceName, instanceId, dbNodeId, database, backupTableName, 1) > 0;
    }

    private void removeDistributedTxStatusCache(Set<String> globalTxIdSet) {
        // replace a new thread with a thread pool so that avoid to create too many new threads
        executorService.execute(() -> {
            txleCache.removeDistributedTxStatusCache(globalTxIdSet);
            globalTxIdSet.clear();
        });
    }

    private void putServerNameIdCategory(TxEvent event) {
        final String globalTxServer = "global-tx-server-info";
        final String serverNameInstanceCategory = event.serviceName() + "__" + event.instanceId() + "__" + event.category();
        boolean result = serverNameIdCategory.add(serverNameInstanceCategory);
        if (result) {
            executorService.execute(() -> {
                int showOrder = dataDictionaryService.selectMaxShowOrder(globalTxServer);
                final DataDictionaryItem ddItem = new DataDictionaryItem(globalTxServer, event.serviceName(), event.instanceId(), event.category(), showOrder + 1, 1, "");
                dataDictionaryService.createDataDictionary(ddItem);
            });
        }
    }

}
