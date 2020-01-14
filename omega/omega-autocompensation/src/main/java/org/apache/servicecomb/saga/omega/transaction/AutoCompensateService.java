/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.github.rholder.retry.Retryer;
import com.google.gson.JsonObject;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandleType;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.ClientAccidentHandlingService;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.apache.servicecomb.saga.omega.transaction.repository.IAutoCompensateDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business interface for auto-compensation.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
@Service
public class AutoCompensateService implements IAutoCompensateService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoCompensateService.class);

    @Autowired
    private IAutoCompensateDao autoCompensateDao;

    @Autowired
    private ClientAccidentHandlingService clientAccidentHandlingService;

    @Autowired
    private Retryer retryer;

    // @Transactional(propagation = Propagation.NOT_SUPPORTED) // Propagation.NOT_SUPPORTED/REQUIRED_NEW indeed is okay, if data are not same among transactions.
    @Override
    public boolean executeAutoCompensateByLocalTxId(String globalTxId, String localTxId) {
        AtomicInteger result = new AtomicInteger(0);
        LOG.error("Executing AutoCompensable method 'executeAutoCompensateByLocalTxId'.");
        try {
            autoCompensateDao.setDataSource(DataSourceMappingCache.get(localTxId));
        } catch (Exception e) {
            LOG.error("Failed to change datasource globalTxId [{}], localTxId [{}].", globalTxId, localTxId, e);
        }
        final List<Map<String, Object>> txleUndoLogList;
        try {
            txleUndoLogList = autoCompensateDao.executeQuery("SELECT * FROM txle_undo_log T WHERE T.globalTxId = ? AND T.localTxId = ? ORDER BY T.lastModifyTime DESC, T.id DESC", globalTxId, localTxId);
        } catch (Exception e) {
            reportMsgToAccidentPlatform(globalTxId, localTxId, "", "Failed to select undo_log info, " + e.getMessage());
            return false;
        }
        if (txleUndoLogList == null || txleUndoLogList.isEmpty()) {
            reportMsgToAccidentPlatform(globalTxId, localTxId, "", "The undo_log info is empty.");
            return false;
        }

        synchronized (AutoCompensateService.class) {
            txleUndoLogList.forEach(map -> {
                String compensateSql = map.get("compensateSql").toString();
                try {
                    checkDataConsistency(compensateSql, globalTxId, localTxId);
                    retryer.call(() -> {
                        if (autoCompensateDao.executeUpdate(compensateSql) > 0) {
                            result.incrementAndGet();
                            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "Successfully to execute AutoCompensable SQL [[{}]]", compensateSql);
                            // update compensation status in txle_undo_log
                            autoCompensateDao.executeUpdate("UPDATE txle_undo_log SET status = " + TxleConstants.Operation.SUCCESS.ordinal() + " WHERE globalTxId = '" + globalTxId + "' and localTxId = '" + localTxId + "'");
                            return true;
                        }
                        return false;
                    });
                } catch (Exception e) {
                    reportMsgToAccidentPlatform(globalTxId, localTxId, compensateSql, "Failed to execute AutoCompensable SQL [" + compensateSql + "], " + e.getMessage());
                }
            });
            return result.get() > 0;
        }
    }

    private boolean checkDataConsistency(String compensateSql, String globalTxId, String localTxId) throws NoSuchAlgorithmException, IOException {
        MySqlStatementParser parser = new MySqlStatementParser(compensateSql);
        SQLStatement sqlStatement = parser.parseStatement();
        if (sqlStatement instanceof MySqlUpdateStatement) {
            MySqlUpdateStatement deleteStatement = (MySqlUpdateStatement) sqlStatement;
            String tableName = deleteStatement.getTableName().toString().toLowerCase();
            String schema = TxleConstants.APP_NAME;
            String txleBackupTableName = "backup_new_" + tableName;
            int backupDataCount = autoCompensateDao.executeQueryCount("SELECT COUNT(1) FROM " + schema + "." + txleBackupTableName + " T WHERE T.globalTxId = ? AND T.localTxId = ? FOR UPDATE", globalTxId, localTxId);
            if (backupDataCount > 0) {
                String pkName = this.parsePrimaryKeyColumnName(tableName);
                int currentDataCount = autoCompensateDao.executeQueryCount("SELECT COUNT(1) FROM " + schema + "." + txleBackupTableName + " T, " + tableName + " T1 WHERE T." + pkName + " = T1." + pkName + " AND T.globalTxId = ? AND T.localTxId = ?", globalTxId, localTxId);
                if (backupDataCount == currentDataCount) {
                    List<Map<String, Object>> columnList = autoCompensateDao.executeQuery(
                            "SELECT GROUP_CONCAT(COLUMN_NAME) COLUMN_NAMES FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + schema + "' AND TABLE_NAME = '" + txleBackupTableName + "' AND COLUMN_NAME NOT IN ('globalTxId', 'localTxId')");
                    if (columnList != null && !columnList.isEmpty()) {
                        StringBuilder columnNames = new StringBuilder();
                        String[] columnArr = columnList.get(0).get("COLUMN_NAMES").toString().split(",");
                        for (String column : columnArr) {
                            if (columnNames.length() == 0) {
                                columnNames.append("T." + column);
                            } else {
                                columnNames.append(",T." + column);
                            }
                        }
                        String backupDataSql = "SELECT " + columnNames + " FROM " + schema + "." + txleBackupTableName + " T WHERE T.globalTxId = '" + globalTxId + "' AND T.localTxId = '" + localTxId + "'";
                        String currentDataSql = "SELECT " + columnNames + " FROM " + tableName + " T, " + schema + "." + txleBackupTableName + " T1 WHERE T." + pkName + " = T1." + pkName + " AND T1.globalTxId = '" + globalTxId + "' AND T1.localTxId = '" + localTxId + "'";

                        String backupDataMD5 = getMD5Digest(backupDataSql, backupDataCount);
                        String currentDataMD5 = getMD5Digest(currentDataSql, backupDataCount);
                        boolean isConsistent = backupDataMD5.equals(currentDataMD5);
                        if (!isConsistent) {
                            throw new RuntimeException("That's not consistent between backup data and current data.");
                        }
                        return isConsistent;
                    }
                }
            }
        }
        return true;
    }

    private String getMD5Digest(String sql, int count) throws NoSuchAlgorithmException, IOException {
        // MD5虽可能会发生数据碰撞，但是极其小的概率，且表中全字段MD5概率就更小了
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        int pageSize = 100;
        for (int i = 0; i < count; i++) {
            // TODO We are not sure that the primary key is a numeric field, so it cannot be a condition to compare. Optimize the 'LIMIT' SQL later.
            List<Map<String, Object>> backupDataList = autoCompensateDao.executeQuery(sql + " LIMIT " + (i * pageSize) + "," + pageSize);
            if (backupDataList == null || backupDataList.isEmpty()) {
                break;
            }
            md5.update(convertToByteFromList(backupDataList));
        }

        byte[] digest = md5.digest();
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            int v = digest[i] & 0xFF;
            if (v < 16) {
                hex.append(0);
            }
            hex.append(Integer.toString(v, 16));
        }
        return hex.toString();
    }

    private String parsePrimaryKeyColumnName(String tableName) {
        String sql = "SHOW FULL COLUMNS FROM " + tableName + TxleConstants.ACTION_SQL;
        // start to mark duration for maintaining sql By Gannalyo.
        ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

        // So far, didn't know how to get primary-key from Druid. So, use the original way.
        List<Map<String, Object>> columnList = autoCompensateDao.executeQuery(sql);

        // end mark duration for maintaining sql By Gannalyo.
        ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

        if (columnList != null && !columnList.isEmpty()) {
            for (Map<String, Object> map : columnList) {
                if ("PRI".equalsIgnoreCase(map.get("Key").toString())) {
                    return map.get("Field").toString();
                }
            }
        }
        return "id";
    }

    private byte[] convertToByteFromList(List<Map<String, Object>> list) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(byteOut);
            for (Map<String, Object> map : list) {
                out.writeObject(map);
            }
            return byteOut.toByteArray();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                byteOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void reportMsgToAccidentPlatform(String globalTxId, String localTxId, String bizinfo, String remark) {
        JsonObject jsonParams = new JsonObject();
        try {
            autoCompensateDao.executeUpdate("UPDATE txle_undo_log SET status = " + TxleConstants.Operation.FAIL.ordinal() + " WHERE globalTxId = '" + globalTxId + "' and localTxId = '" + localTxId + "'");
            jsonParams.addProperty("type", AccidentHandleType.ROLLBACK_ERROR.toInteger());
            jsonParams.addProperty("globaltxid", globalTxId);
            jsonParams.addProperty("localtxid", localTxId);
            jsonParams.addProperty("bizinfo", bizinfo);
            jsonParams.addProperty("remark", remark);
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to execute AutoCompensable method jsonParams [{}]", jsonParams.toString());
            clientAccidentHandlingService.reportMsgToAccidentPlatform(jsonParams.toString());
            // Do not throw an exception.
            // throw new RuntimeException(TxleConstants.logErrorPrefixWithTime() + "Failed to execute AutoCompensable SQL [" + compensateSql + "], result [" + tempResult + "]");
        } catch (Exception e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to report accident for method 'executeAutoCompensateByLocalTxId', params [{}].", jsonParams.toString(), e);
        }
    }

}