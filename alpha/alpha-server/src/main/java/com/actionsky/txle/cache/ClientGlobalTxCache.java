/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.cache;

import com.actionsky.txle.grpc.TxleTransactionStart;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.common.TxleConstants;

import java.io.Serializable;

/**
 * 全局事务对应的客户端信息缓存
 * 缓存为不可变缓存，可变缓存(如事务状态)不适合分布式场景
 * 如果某节点没有缓存会从数据库读取一次，但仅读取一次即可，因此采用本地缓存
 */
public class ClientGlobalTxCache implements Serializable {
    private String globalTxId;
    private String serviceName;
    private String instanceId;
    private String serviceCategory;
    private long timeout;

    public ClientGlobalTxCache(TxleTransactionStart txStart) {
        this.globalTxId = txStart.getGlobalTxId();
        this.serviceName = txStart.getServiceName();
        this.instanceId = TxleConstants.getServiceInstanceId(this.serviceName, txStart.getServiceIP());
        this.serviceCategory = txStart.getServiceCategory();
        this.timeout = txStart.getTimeout();
    }

    public ClientGlobalTxCache(TxEvent event) {
        this.globalTxId = event.globalTxId();
        this.serviceName = event.serviceName();
        this.instanceId = event.instanceId();
        this.serviceCategory = event.category();
        this.timeout = (event.expiryTime().getTime() - event.creationTime().getTime()) / 1000;
    }

    public String getGlobalTxId() {
        return globalTxId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public long getTimeout() {
        return timeout;
    }
}

