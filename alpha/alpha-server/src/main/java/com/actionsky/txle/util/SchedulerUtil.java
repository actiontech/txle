/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.util;

import com.actionsky.txle.cache.ITxleConsistencyCache;
import org.apache.servicecomb.saga.alpha.core.TxleConsulClient;
import org.apache.servicecomb.saga.alpha.core.datatransfer.IDataTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

/**
 * @author Gannalyo
 * @since 2020/5/12
 */
public class SchedulerUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerUtil.class);

    @Autowired
    private TxleConsulClient txleConsulClient;

    @Autowired
    private IDataTransferService dataTransferService;

    @Resource(name = "txleMysqlCache")
    @Autowired
    private ITxleConsistencyCache consistencyCache;

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledTask() {
        if (txleConsulClient.isMaster()) {
            LOG.info("Triggered data transfer task on current master node.");
            dataTransferService.dataTransfer("TxEvent");
            // 每天零点执行，删除已经异常或结束全局事务对应的缓存，删除超时缓存
            consistencyCache.clearExpiredAndOverTxCache();
        } else {
            LOG.info("Could not trigger data transfer task, because current node had been not master yet.");
        }
    }

}
