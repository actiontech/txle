/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.grpc.interfaces;

import com.actionsky.txle.cache.ITxleEhCache;
import com.actionsky.txle.cache.TxleCacheType;
import com.actionsky.txle.grpc.TxleSubTransactionStart;
import com.actionsky.txle.grpc.TxleSubTxSql;
import com.actionsky.txle.grpc.TxleTransactionStart;
import com.actionsky.txle.grpc.TxleTxStartAck;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Gannalyo
 * @since 2020/2/25
 */
public class CompensateService {
    private static final Logger LOG = LoggerFactory.getLogger(CompensateService.class);
    private final String schema = TxleConstants.APP_NAME;

    @Autowired
    private ICustomRepository customRepository;

    @Autowired
    private IKafkaMessageRepository kafkaMessageRepository;

    @Autowired
    private ITxleEhCache txleEhCache;

    public void prepareBackupSql(TxleTransactionStart tx, TxleTxStartAck.Builder txStartAck, boolean isExistsGlobalTx, Map<String, String> localTxBackupSql) {
        for (TxleSubTransactionStart subTx : tx.getSubTxInfoList()) {
            try {
                SQLStatement sqlStatement = new MySqlStatementParser(subTx.getSql()).parseStatement();
                String tableName = parseTableName(sqlStatement);
                String tableNameWithSchema = tableName;
                if (tableName.indexOf(".") < 0) {
                    tableNameWithSchema = subTx.getDbSchema() + "." + tableName;
                } else {
                    tableName = tableName.substring(tableName.indexOf(".") + 1);
                }
                String txleOldBackupTableName = TxleConstants.giveBackupTableNameForOldData(subTx.getDbSchema(), tableName), txleOldBackupTableNameWithSchema = this.schema + "." + txleOldBackupTableName;
                String txleNewBackupTableName = TxleConstants.giveBackupTableNameForNewData(subTx.getDbSchema(), tableName), txleNewBackupTableNameWithSchema = this.schema + "." + txleNewBackupTableName;

                // create backup table & alter structure
                TxleSubTxSql.Builder subTxSql = TxleSubTxSql.newBuilder().setLocalTxId(subTx.getLocalTxId()).setDbNodeId(subTx.getDbNodeId()).setDbSchema(subTx.getDbSchema()).setOrder(subTx.getOrder());
                this.constructBackupSqls(tx, isExistsGlobalTx, subTx, subTxSql, tableNameWithSchema, txleOldBackupTableName, txleOldBackupTableNameWithSchema, txleNewBackupTableName, txleNewBackupTableNameWithSchema);

                String operation = "";
                if (sqlStatement instanceof MySqlInsertStatement) {
                    operation = "insert";
                    // the formal business sql
                    subTxSql.addSubTxSql(subTx.getSql());
                    // the backup new data sql
                    Object primaryKey = this.txleEhCache.get(TxleCacheType.INIT, subTx.getDbNodeId() + "." + tableNameWithSchema);
                    if (primaryKey == null) {
                        primaryKey = "id";
                    }
                    subTxSql.addSubTxSql(String.format("INSERT INTO " + txleNewBackupTableNameWithSchema + " SELECT *, '%s', '%s' FROM %s WHERE " + primaryKey + " = (SELECT LAST_INSERT_ID()) FOR UPDATE " + TxleConstants.ACTION_SQL, tx.getGlobalTxId(), subTx.getLocalTxId(), tableNameWithSchema));
                } else if (sqlStatement instanceof MySqlDeleteStatement) {
                    operation = "delete";
                    subTxSql.addSubTxSql(String.format("INSERT INTO " + txleOldBackupTableNameWithSchema + " SELECT *, '%s', '%s' FROM %s WHERE %s FOR UPDATE "
                            + TxleConstants.ACTION_SQL, tx.getGlobalTxId(), subTx.getLocalTxId(), tableNameWithSchema, ((MySqlDeleteStatement) sqlStatement).getWhere().toString()));
                    subTxSql.addSubTxSql(subTx.getSql());
                } else if (sqlStatement instanceof MySqlUpdateStatement) {
                    operation = "update";
                    subTxSql.addSubTxSql(String.format("INSERT INTO " + txleOldBackupTableNameWithSchema + " SELECT *, '%s', '%s' FROM %s WHERE %s FOR UPDATE "
                            + TxleConstants.ACTION_SQL, tx.getGlobalTxId(), subTx.getLocalTxId(), tableNameWithSchema, ((MySqlUpdateStatement) sqlStatement).getWhere().toString()));
                    subTxSql.addSubTxSql(subTx.getSql());
                    subTxSql.addSubTxSql(String.format("INSERT INTO " + txleNewBackupTableNameWithSchema + " SELECT *, '%s', '%s' FROM %s WHERE %s FOR UPDATE "
                            + TxleConstants.ACTION_SQL, tx.getGlobalTxId(), subTx.getLocalTxId(), tableNameWithSchema, ((MySqlUpdateStatement) sqlStatement).getWhere().toString()));
                }

                if (!txleEhCache.readConfigCache(TxleConstants.getServiceInstanceId(tx.getServiceName(), tx.getServiceIP()), tx.getServiceCategory(), ConfigCenterType.ClientCompensate)) {
                    subTxSql.addSubTxSql("set autocommit=0");
                    subTxSql.addSubTxSql("commit");
                    subTxSql.addSubTxSql("set autocommit=1");
//                    subTxSql.addSubTxSql("begin");
                }

                // return SQLs to client
                txStartAck.addSubTxSql(subTxSql.build());
                final StringBuilder backupSqls = new StringBuilder();
                subTxSql.getSubTxSqlList().forEach(sql -> backupSqls.append(sql + TxleConstants.STRING_SEPARATOR));
                localTxBackupSql.put(subTx.getLocalTxId(), backupSqls.toString());

                kafkaMessageRepository.save(new KafkaMessage(tx.getGlobalTxId(), subTx.getLocalTxId(), "", subTx.getDbNodeId(), "", tableNameWithSchema, operation, ""));
            } catch (Exception e) {
                handleExceptionWithFaultToleranceChecking("Failed to prepare sqls for backup.", e, tx, txStartAck);
            }
        }
    }

    private String parseTableName(SQLStatement sqlStatement) {
        if (sqlStatement instanceof MySqlInsertStatement) {
            return ((MySqlInsertStatement) sqlStatement).getTableName().toString();
        } else if (sqlStatement instanceof MySqlDeleteStatement) {
            return ((MySqlDeleteStatement) sqlStatement).getTableName().toString();
        } else if (sqlStatement instanceof MySqlUpdateStatement) {
            return ((MySqlUpdateStatement) sqlStatement).getTableName().toString();
        }
        return "";
    }

    private void constructBackupSqls(TxleTransactionStart tx, boolean isExistsGlobalTx, TxleSubTransactionStart subTx,
                                     TxleSubTxSql.Builder subTxSql, String tableNameWithSchema, String txleOldBackupTableName,
                                     String txleOldBackupTableNameWithSchema, String txleNewBackupTableName, String txleNewBackupTableNameWithSchema) {
        String serviceInstanceId = TxleConstants.getServiceInstanceId(tx.getServiceName(), tx.getServiceIP());
        if (!isExistsGlobalTx) {
            // record the information of backup table to db after ending global transaction
            String backupTableKey = subTx.getDbNodeId() + "_" + subTx.getDbSchema() + "_" + txleOldBackupTableName;
            Boolean isExecutedBackupTable = txleEhCache.getBooleanValue(TxleCacheType.OTHER, backupTableKey);
            if ((isExecutedBackupTable != null && isExecutedBackupTable) || this.checkIsExistsBackupTable(tx.getServiceName(), serviceInstanceId, subTx.getDbNodeId(), subTx.getDbSchema(), txleOldBackupTableName)) {
                return;
            }

            subTxSql.addSubTxSql("CREATE DATABASE IF NOT EXISTS " + this.schema + " DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci");
            subTxSql.addSubTxSql("CREATE TABLE IF NOT EXISTS " + txleOldBackupTableNameWithSchema + " AS SELECT * FROM " + tableNameWithSchema + " LIMIT 0");
            subTxSql.addSubTxSql("DROP PROCEDURE IF EXISTS alter_txle_backup_table;\n" +
                    "DELIMITER $$\n" +
                    "create procedure alter_txle_backup_table()\n" +
                    "         begin\n" +
                    "             if not exists (SELECT 1 FROM information_schema.COLUMNS WHERE COLUMN_NAME = 'globalTxId' AND TABLE_NAME = " + txleOldBackupTableName + " AND TABLE_SCHEMA = '" + this.schema + "') then\n" +
                    "\t\t\t\t\t\t\t\t\tALTER TABLE " + this.schema + "." + txleOldBackupTableName + " ADD globalTxId VARCHAR(36);\n" +
                    "\t\t\t\t\t\t\t\t\tALTER TABLE " + this.schema + "." + txleOldBackupTableName + " ADD localTxId VARCHAR(36);\n" +
                    "\t\t\t\t\t\t\tend if;\n" +
                    "         end$$\n" +
                    "DELIMITER;\n" +
                    "CALL alter_txle_backup_table();\n" +
                    "DROP PROCEDURE IF EXISTS alter_txle_backup_table;");
            // it's convenient to handler the procedure for client
            subTxSql.setProcedureNumber(1);
            subTxSql.addSubTxSql("CREATE TABLE IF NOT EXISTS " + txleNewBackupTableNameWithSchema + " AS SELECT * FROM " + txleOldBackupTableNameWithSchema + " LIMIT 0");

            // 此处先设置这个缓存，待此全局事务结束后，会清除次缓存，并设置backupTableKey缓存，后续其它事务请求以backupTableKey缓存判断
            Object cacheValue = txleEhCache.get(TxleCacheType.OTHER, "is-executed-backup-table-" + tx.getGlobalTxId());
            List<String[]> cacheList = cacheValue == null ? new ArrayList<>() : (List<String[]>) cacheValue;
            cacheList.add(new String[]{subTx.getDbNodeId(), subTx.getDbSchema(), txleOldBackupTableName, txleNewBackupTableName});
            txleEhCache.put(TxleCacheType.OTHER, "is-executed-backup-table-" + tx.getGlobalTxId(), cacheList, 300);
        }
    }

    public void constructCompensateSql(TxleTransactionStart tx, TxleTxStartAck.Builder txStartAck, Map<String, String> localTxCompensateSql) {
        for (TxleSubTransactionStart subTx : tx.getSubTxInfoList()) {
            try {
                SQLStatement sqlStatement = new MySqlStatementParser(subTx.getSql()).parseStatement();
                String tableName = parseTableName(sqlStatement);
                String tableNameWithSchema = tableName;
                if (tableName.indexOf(".") < 0) {
                    tableNameWithSchema = subTx.getDbSchema() + "." + tableName;
                } else {
                    tableName = tableName.substring(tableName.indexOf(".") + 1);
                }
                String txleOldBackupTableName = TxleConstants.giveBackupTableNameForOldData(subTx.getDbSchema(), tableName);
                String txleNewBackupTableName = this.schema + "." + TxleConstants.giveBackupTableNameForNewData(subTx.getDbSchema(), tableName);

                String compensateSql = "";
                if (sqlStatement instanceof MySqlInsertStatement) {
                    compensateSql = String.format("DELETE FROM " + tableNameWithSchema + " WHERE id IN (SELECT id FROM " + txleNewBackupTableName + " WHERE globalTxId = '%s' AND localTxId = '%s') " + TxleConstants.ACTION_SQL, tx.getGlobalTxId(), subTx.getLocalTxId());
                } else if (sqlStatement instanceof MySqlDeleteStatement) {
                    compensateSql = String.format("INSERT INTO " + tableNameWithSchema + " SELECT %s FROM " + schema + "." + txleOldBackupTableName + " WHERE globalTxId = '%s' AND localTxId = '%s' "
                            + TxleConstants.ACTION_SQL, this.readColumnNames(subTx.getDbSchema(), tableName), tx.getGlobalTxId(), subTx.getLocalTxId());
                } else if (sqlStatement instanceof MySqlUpdateStatement) {
                    String setColumns = this.constructSetColumnsForUpdate(subTx.getDbSchema(), tableName);
                    Object primaryKey = this.txleEhCache.get(TxleCacheType.INIT, subTx.getDbNodeId() + "." + tableNameWithSchema);
                    if (primaryKey == null) {
                        List list = customRepository.executeQuery("SELECT T.field FROM BusinessDBLatestDetail T WHERE T.isprimarykey = 1 AND T.node = ? AND T.dbschema = ? AND T.tablename = ?", subTx.getDbNodeId(), subTx.getDbSchema(), tableName);
                        if (list != null && !list.isEmpty()) {
                            primaryKey = list.get(0);
                            if (primaryKey != null) {
                                txleEhCache.put(TxleCacheType.INIT, subTx.getDbNodeId() + "." + subTx.getDbSchema() + "." + tableName, primaryKey);
                            }
                        }
                    }
                    if (primaryKey == null) {
                        primaryKey = "id";
                    }
                    // construct reversed sql
                    compensateSql = String.format("UPDATE %s T INNER JOIN %s T1 ON T." + primaryKey + " = T1." + primaryKey + " SET %s WHERE T1.globalTxId = '%s' AND T1.localTxId = '%s' "
                            + TxleConstants.ACTION_SQL, tableNameWithSchema, this.schema + "." + txleOldBackupTableName, setColumns, tx.getGlobalTxId(), subTx.getLocalTxId());
                }
                localTxCompensateSql.put(subTx.getLocalTxId(), compensateSql);
            } catch (Exception e) {
                handleExceptionWithFaultToleranceChecking("Failed to construct sql for compensation.", e, tx, txStartAck);
            }
        }
    }

    private void handleExceptionWithFaultToleranceChecking(String cause, Throwable e, TxleTransactionStart tx, TxleTxStartAck.Builder txStartAck) {
        LOG.error(cause, e);
        if (txleEhCache.readConfigCache(TxleConstants.getServiceInstanceId(tx.getServiceName(), tx.getServiceIP()), tx.getServiceCategory(), ConfigCenterType.GlobalTxFaultTolerant)) {
            txStartAck.setStatus(TxleTxStartAck.TransactionStatus.FAULTTOLERANT);
            LOG.error(cause);
        } else {
            txStartAck.setStatus(TxleTxStartAck.TransactionStatus.ABORTED);
            throw new RuntimeException(cause, e);
        }
    }

    private String readColumnNames(String bizDBSchema, String bizTableName) {
        try {
            List fieldList = this.customRepository.executeQuery("SELECT GROUP_CONCAT(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + bizDBSchema + "' AND TABLE_NAME = '" + bizTableName + "' AND COLUMN_NAME NOT IN ('globalTxId', 'localTxId')");
            return fieldList.get(0).toString();
        } catch (Exception e) {
            throw new RuntimeException("Do not exist column information for schema '" + bizDBSchema + "', table '" + bizTableName + "'", e);
        }
    }

    private String constructSetColumnsForUpdate(String bizDBSchema, String bizTableName) {
        String fieldNames = this.readColumnNames(bizDBSchema, bizTableName);
        // set fields for update sql
        String[] fieldNameArr = fieldNames.split(",");
        StringBuilder setColumns = new StringBuilder();
        for (String fieldName : fieldNameArr) {
            if (setColumns.length() == 0) {
                setColumns.append("T." + fieldName + " = T1." + fieldName);
            } else {
                setColumns.append(", T." + fieldName + " = T1." + fieldName);
            }
        }
        return setColumns.toString();
    }

    private boolean checkIsExistsBackupTable(String serviceName, String instanceId, String dbNodeId, String database, String backupTableName) {
        String sql = "SELECT COUNT(1) FROM BusinessDBBackupInfo T WHERE T.servicename = ? AND T.instanceid = ? AND T.dbnodeid = ? AND T.dbschema = ? AND T.backuptablename = ? AND T.status = ?";
        return this.customRepository.count(sql, serviceName, instanceId, dbNodeId, database, backupTableName, 1) > 0;
    }
}
