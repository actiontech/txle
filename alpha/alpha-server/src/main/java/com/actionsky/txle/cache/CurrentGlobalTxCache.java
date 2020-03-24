/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.cache;

import com.actionsky.txle.grpc.TxleSubTransactionStart;
import com.actionsky.txle.grpc.TxleTransactionStart;
import com.actionsky.txle.grpc.TxleTxStartAck;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.common.TxleConstants;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * 缓存务必与数据库信息保持一致，即业务运行过程中强依赖缓存，提高性能，若缓存数据有一丁点不准确，则将无法保证数据的正确性，若无缓存即数据库也无对应数据，否则再查数据库就失去了缓存意义
 *
 * @author Gannalyo
 * @since 2020/2/17
 */
public class CurrentGlobalTxCache implements Serializable {
    private String serviceName;
    private String instanceId;
    private String serviceCategory;
    private String globalTxId;
    private long timeout;
    private TxleTxStartAck.TransactionStatus status;
    private LinkedHashSet<SubTxCacheEntity> subTxCacheEntities;

    public CurrentGlobalTxCache(TxleTransactionStart txStart) {
        this.serviceName = txStart.getServiceName();
        this.instanceId = TxleConstants.getServiceInstanceId(this.serviceName, txStart.getServiceIP());
        this.serviceCategory = txStart.getServiceCategory();
        this.globalTxId = txStart.getGlobalTxId();
        this.timeout = txStart.getTimeout();
        this.status = TxleTxStartAck.TransactionStatus.NORMAL;

        if (subTxCacheEntities == null) {
            subTxCacheEntities = new LinkedHashSet<>();
        }
        txStart.getSubTxInfoList().forEach(subTx -> subTxCacheEntities.add(new SubTxCacheEntity(subTx)));
    }

    public CurrentGlobalTxCache(TxEvent event) {
        this.serviceName = event.serviceName();
        this.instanceId = event.instanceId();
        this.serviceCategory = event.category();
        this.timeout = (event.expiryTime().getTime() - event.creationTime().getTime()) / 1000;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public String getGlobalTxId() {
        return globalTxId;
    }

    public void setGlobalTxId(String globalTxId) {
        this.globalTxId = globalTxId;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public TxleTxStartAck.TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TxleTxStartAck.TransactionStatus status) {
        this.status = status;
    }

    public LinkedHashSet<SubTxCacheEntity> getSubTxCacheEntities() {
        return subTxCacheEntities;
    }

    public String getBackupSqls(String localTxId) {
        Iterator<SubTxCacheEntity> entityIterator = this.subTxCacheEntities.iterator();
        while (entityIterator.hasNext()) {
            SubTxCacheEntity subTxCacheEntity = entityIterator.next();
            if (subTxCacheEntity.getSubTxStart().getLocalTxId().equals(localTxId)) {
                final StringBuilder backupSql = new StringBuilder();
                subTxCacheEntity.getBackupSqls().forEach(sql -> backupSql.append(sql + TxleConstants.STRING_SEPARATOR));
                return backupSql.toString();
            }
        }
        return "";
    }

    public void setSubTxInfo(String localTxId, String tableName, LinkedHashSet<String> backupSqls) {
        for (SubTxCacheEntity subCache : this.subTxCacheEntities) {
            if (subCache.getSubTxStart().getLocalTxId().equals(localTxId)) {
                subCache.setTableName(tableName);
                subCache.setBackupSqls(backupSqls);
                break;
            }
        }
    }

    public String getSubTxCompensateSql(String localTxId) {
        Iterator<SubTxCacheEntity> entityIterator = this.subTxCacheEntities.iterator();
        while (entityIterator.hasNext()) {
            SubTxCacheEntity subTxCacheEntity = entityIterator.next();
            if (subTxCacheEntity.getSubTxStart().getLocalTxId().equals(localTxId)) {
                return subTxCacheEntity.getCompensateSql();
            }
        }
        return "";
    }

    public void setCompensateSql(String localTxId, String compensateSql) {
        for (SubTxCacheEntity subCache : this.subTxCacheEntities) {
            if (subCache.getSubTxStart().getLocalTxId().equals(localTxId)) {
                subCache.setCompensateSql(compensateSql);
                break;
            }
        }
    }

    public void addSubTx(TxleSubTransactionStart subTx) {
        this.subTxCacheEntities.add(new SubTxCacheEntity(subTx));
    }
}

class SubTxCacheEntity implements Serializable {
    private TxleSubTransactionStart subTxStart;
    private String tableName;
    private LinkedHashSet<String> backupSqls;
    private String compensateSql;

    SubTxCacheEntity(TxleSubTransactionStart subTxStart) {
        this.subTxStart = subTxStart;
    }

    public TxleSubTransactionStart getSubTxStart() {
        return subTxStart;
    }

    public void setSubTxStart(TxleSubTransactionStart subTxStart) {
        this.subTxStart = subTxStart;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public LinkedHashSet<String> getBackupSqls() {
        return backupSqls;
    }

    public void setBackupSqls(LinkedHashSet<String> backupSqls) {
        this.backupSqls = backupSqls;
    }

    public String getCompensateSql() {
        return compensateSql;
    }

    public void setCompensateSql(String compensateSql) {
        this.compensateSql = compensateSql;
    }
}
