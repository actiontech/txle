/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.eventaddition;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "TxEventAddition")
public class TxEventAddition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surrogateId;

    private String serviceName;
    private String instanceId;
    private String globalTxId;
    private String localTxId;
    private String dbNodeId;
    private String dbSchema;
    private String businessSql;
    private String backupSql;
    private String compensateSql;
    private int compensateStatus;
    private int executeOrder;
    private Date creationTime;

    private TxEventAddition() {
    }

    public TxEventAddition(String serviceName, String instanceId, String globalTxId, String localTxId, String dbNodeId, String dbSchema, String businessSql, String backupSql, String compensateSql, int executeOrder) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.globalTxId = globalTxId;
        this.localTxId = localTxId;
        this.dbNodeId = dbNodeId;
        this.dbSchema = dbSchema;
        this.businessSql = businessSql;
        this.backupSql = backupSql;
        this.compensateSql = compensateSql;
        this.executeOrder = executeOrder;
        this.creationTime = new Date(System.currentTimeMillis());
    }

    public Long getSurrogateId() {
        return surrogateId;
    }

    public void setSurrogateId(Long surrogateId) {
        this.surrogateId = surrogateId;
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

    public String getGlobalTxId() {
        return globalTxId;
    }

    public void setGlobalTxId(String globalTxId) {
        this.globalTxId = globalTxId;
    }

    public String getLocalTxId() {
        return localTxId;
    }

    public void setLocalTxId(String localTxId) {
        this.localTxId = localTxId;
    }

    public String getDbNodeId() {
        return dbNodeId;
    }

    public void setDbNodeId(String dbNodeId) {
        this.dbNodeId = dbNodeId;
    }

    public String getDbSchema() {
        return dbSchema;
    }

    public void setDbSchema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

    public String getBusinessSql() {
        return businessSql;
    }

    public void setBusinessSql(String businessSql) {
        this.businessSql = businessSql;
    }

    public String getBackupSql() {
        return backupSql;
    }

    public void setBackupSql(String backupSql) {
        this.backupSql = backupSql;
    }

    public String getCompensateSql() {
        return compensateSql;
    }

    public void setCompensateSql(String compensateSql) {
        this.compensateSql = compensateSql;
    }

    public int getCompensateStatus() {
        return compensateStatus;
    }

    public void setCompensateStatus(int compensateStatus) {
        this.compensateStatus = compensateStatus;
    }

    public int getExecuteOrder() {
        return executeOrder;
    }

    public void setExecuteOrder(int executeOrder) {
        this.executeOrder = executeOrder;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public String toString() {
        return "TxEventAddition{" +
                "surrogateId=" + surrogateId +
                ", serviceName='" + serviceName + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", globalTxId='" + globalTxId + '\'' +
                ", localTxId='" + localTxId + '\'' +
                ", dbNodeId='" + dbNodeId + '\'' +
                ", dbSchema='" + dbSchema + '\'' +
                ", businessSql='" + businessSql + '\'' +
                ", backupSql=" + backupSql +
                ", compensateSql='" + compensateSql + '\'' +
                ", compensateStatus='" + compensateStatus + '\'' +
                ", executeOrder='" + executeOrder + '\'' +
                ", creationTime=" + creationTime +
                '}';
    }

}
