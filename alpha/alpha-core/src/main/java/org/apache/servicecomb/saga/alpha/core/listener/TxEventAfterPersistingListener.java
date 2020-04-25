/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.listener;

import com.actionsky.txle.cache.ITxleConsistencyCache;
import com.actionsky.txle.cache.ITxleEhCache;
import com.actionsky.txle.cache.TxleCacheType;
import com.actionsky.txle.enums.GlobalTxStatus;
import com.actionsky.txle.grpc.interfaces.ICustomRepository;
import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxleMetrics;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.servicecomb.saga.common.EventType.*;

/**
 * @author Gannalyo
 * @since 2019-08-30
 */
public class TxEventAfterPersistingListener implements Observer {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private IKafkaMessageProducer kafkaMessageProducer;

    @Autowired
    private IDataDictionaryService dataDictionaryService;

    @Autowired
    private ICustomRepository customRepository;

    @Autowired
    private ITxleEhCache txleEhCache;

    @Resource(name = "txleMysqlCache")
    @Autowired
    private ITxleConsistencyCache consistencyCache;

    @Autowired
    private TxleMetrics txleMetrics;

    private final Set<String> serverNameIdCategory = new HashSet<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    public void update(Observable arg0, Object arg1) {
        long a = System.currentTimeMillis();
        if (arg0 != null) {
            if (arg0 instanceof GlobalTxListener) {
                TxEvent event = ((GlobalTxListener) arg0).getEvent();
                try {
                    if (event != null) {
                        if (event.id() == null || event.id() == -1) {
                            executorService.execute(() -> {
                                txleMetrics.startMarkTxDuration(event);
                                txleMetrics.countTxNumber(event);
                            });
                        } else {
                            executorService.execute(() -> txleMetrics.endMarkTxDuration(event));

//                            log.info("The listener [{}] observes the new event [" + event.toString() + "].", this.getClass());
                            if (SagaStartedEvent.name().equals(event.type())) {
                                // increase 1 for the minimum identify of undone event when some global transaction starts.
                                EventScanner.UNENDED_MIN_EVENT_ID_SELECT_COUNT.incrementAndGet();
                                this.setServerNameIdCategory(event);
                            } else if (TxStartedEvent.name().equals(event.type())) {
                                this.setServerNameIdCategory(event);
                            } else if (TxAbortedEvent.name().equals(event.type())) {
                                consistencyCache.setKeyValueCache(TxleConstants.constructTxStatusCacheKey(event.globalTxId()), GlobalTxStatus.Aborted.toString());
                            } else if (SagaEndedEvent.name().equals(event.type())) {
                                executorService.execute(() -> {
                                    // remove local cache for current global tx
                                    txleEhCache.removeGlobalTxCache(event.globalTxId());
                                    // remove distribution cache for current global tx
                                    consistencyCache.deleteByKeyPrefix(TxleConstants.constructTxCacheKey(event.globalTxId()));

                                    kafkaMessageProducer.send(event);
                                    this.saveBusinessDBBackupInfo(event);
                                });
                            }
                        }
//                        log.info("\r\n---- [{}] listener after saving [{}]，globalTxId = [{}], localTxId = [{}], millisecond [{}].", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()), event.type(), event.globalTxId(), event.localTxId(), (System.currentTimeMillis() - a));
                    }
                } catch (Exception e) {
                    log.error("Failed to execute listener after persisting event. globalTxId = {}, localTxId = {}", event.globalTxId(), event.localTxId(), e);
                }
            }
        }
    }

    private void saveBusinessDBBackupInfo(TxEvent event) {
        Object cacheValue = txleEhCache.get(TxleCacheType.OTHER, "is-executed-backup-table-" + event.globalTxId());
        if (cacheValue != null) {
            List<String[]> cacheList = (List<String[]>) cacheValue;
            if (!cacheList.isEmpty()) {
                cacheList.forEach(backupInfo -> {
                    String backupTableKey = backupInfo[0] + "_" + backupInfo[1] + "_" + backupInfo[2];
                    Boolean isExecutedBackupTable = txleEhCache.getBooleanValue(TxleCacheType.OTHER, backupTableKey);
                    if (isExecutedBackupTable == null || !isExecutedBackupTable) {
                        if (!this.checkIsExistsBackupTable(event.serviceName(), event.instanceId(), backupInfo[0], backupInfo[1], backupInfo[2])) {
                            this.customRepository.executeUpdate("INSERT INTO BusinessDBBackupInfo (servicename, instanceid, dbnodeid, dbschema, backuptablename) VALUES (?, ?, ?, ?, ?)", event.serviceName(), event.instanceId(), backupInfo[0], backupInfo[1], backupInfo[2]);
                            this.customRepository.executeUpdate("INSERT INTO BusinessDBBackupInfo (servicename, instanceid, dbnodeid, dbschema, backuptablename) VALUES (?, ?, ?, ?, ?)", event.serviceName(), event.instanceId(), backupInfo[0], backupInfo[1], backupInfo[3]);
                        }
                        txleEhCache.put(TxleCacheType.OTHER, backupTableKey, true);
                    }
                });
            }
        }
    }

    private boolean checkIsExistsBackupTable(String serviceName, String instanceId, String dbNodeId, String database, String backupTableName) {
        String sql = "SELECT COUNT(1) FROM BusinessDBBackupInfo T WHERE T.servicename = ? AND T.instanceid = ? AND T.dbnodeid = ? AND T.dbschema = ? AND T.backuptablename = ? AND T.status = ?";
        return this.customRepository.count(sql, serviceName, instanceId, dbNodeId, database, backupTableName, 1) > 0;
    }

    private void setServerNameIdCategory(TxEvent event) {
        executorService.execute(() -> {
            final String serverNameInstanceCategory = event.serviceName() + "__" + event.instanceId() + "__" + event.category();
            if (!serverNameIdCategory.contains(serverNameInstanceCategory)) {
                serverNameIdCategory.add(serverNameInstanceCategory);
                final String globalTxServer = "global-tx-server-info";
                int showOrder = dataDictionaryService.selectMaxShowOrder(globalTxServer);
                final DataDictionaryItem ddItem = new DataDictionaryItem(globalTxServer, event.serviceName(), event.instanceId(), event.category(), showOrder + 1, 1, "");
                dataDictionaryService.createDataDictionary(ddItem);
            }
        });
    }

}
