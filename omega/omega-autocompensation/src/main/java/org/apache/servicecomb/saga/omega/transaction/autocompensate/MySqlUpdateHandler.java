/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * 更新数据逻辑
 * 1.更新前，备份影响数据到对应的备份数据表
 * 2.执行更新数据操作
 * 3.更新后，备份影响数据到对应的备份数据表
 * 4.更新数据操作与生成补偿备份操作在同一事务中，防止脏写，即补偿备份前后或删除数据前后，避免其它业务对涉及数据进行更新
 * 5.后续需要补偿时，先对比更新后的备份数据与数据库中的数据是否完全一致，若完全一致则直接执行补偿SQL，若非完全一致则放弃补偿，直接上报差错
 */
public class MySqlUpdateHandler extends AutoCompensateUpdateHandler {

    private static volatile MySqlUpdateHandler mySqlUpdateHandler = null;
    private static final Logger LOG = LoggerFactory.getLogger(MySqlUpdateHandler.class);

    public static MySqlUpdateHandler newInstance() {
        if (mySqlUpdateHandler == null) {
            synchronized (MySqlUpdateHandler.class) {
                if (mySqlUpdateHandler == null) {
                    mySqlUpdateHandler = new MySqlUpdateHandler();
                }
            }
        }
        return mySqlUpdateHandler;
    }

    @Override
    public boolean prepareCompensationBeforeUpdating(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String globalTxId, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
        Connection connection;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            MySqlUpdateStatement deleteStatement = (MySqlUpdateStatement) sqlStatement;
            // 1.take table's name out
            SQLName table = deleteStatement.getTableName();
            String tableName = table.toString().toLowerCase();
            String schema = TxleConstants.APP_NAME;
            String txleBackupTableName = "backup_old_" + tableName;
            standbyParams.put("tablename", tableName);
            standbyParams.put("operation", "update");

            // 2.take conditions out
            SQLExpr where = deleteStatement.getWhere();
            // It doesn't matter, even though the 'where-sql' contains a line break.
            String whereSql = where.toString();
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - table: [{}] - where: [{}].", Thread.currentThread().getId(), tableName, whereSql);

            // 3.create backup table
            connection = delegate.getConnection();
            this.prepareBackupTable(connection, tableName, txleBackupTableName);

            // 4.backup data
            String backupDataSql = String.format("INSERT INTO " + schema + "." + txleBackupTableName + " SELECT *, '%s', '%s' FROM %s WHERE %s FOR UPDATE " + TxleConstants.ACTION_SQL, globalTxId, localTxId, tableName, whereSql);
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - backupDataSql: [{}].", Thread.currentThread().getId(), backupDataSql);
            int backupResult = connection.prepareStatement(backupDataSql).executeUpdate();
            if (backupResult > 0) {
                // 5.construct compensateSql
                preparedStatement = connection.prepareStatement("SELECT GROUP_CONCAT(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + schema + "' AND TABLE_NAME = '" + txleBackupTableName + "' AND COLUMN_NAME NOT IN ('globalTxId', 'localTxId')");
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    String[] fieldNameArr = resultSet.getString(1).split(",");
                    StringBuilder setColumns = new StringBuilder();
                    for (String fieldName : fieldNameArr) {
                        if (setColumns.length() == 0) {
                            setColumns.append("T." + fieldName + " = T1." + fieldName);
                        } else {
                            setColumns.append(", T." + fieldName + " = T1." + fieldName);
                        }
                    }

                    // take primary-key name
                    String primaryKeyColumnName = this.parsePrimaryKeyColumnName(delegate, tableName);
                    String compensateSql = String.format("UPDATE %s T INNER JOIN %s T1 ON T." + primaryKeyColumnName + " = T1." + primaryKeyColumnName + " SET %s WHERE T1.globalTxId = '%s' AND T1.localTxId = '%s' "
                            + TxleConstants.ACTION_SQL, tableName, schema + "." + txleBackupTableName, setColumns.toString(), globalTxId, localTxId);

                    // 6.save txle_undo_log
                    return this.saveTxleUndoLog(delegate, globalTxId, localTxId, executeSql, compensateSql, server);
                }
            }
            return false;
        } catch (SQLException e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for update sql.", e);
            throw e;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for update SQL.", e);
                }
            }
        }
    }

    @Override
    public boolean prepareCompensationAfterUpdating(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String globalTxId, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
        Connection connection;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            MySqlUpdateStatement deleteStatement = (MySqlUpdateStatement) sqlStatement;
            // 1.take table's name out
            SQLName table = deleteStatement.getTableName();
            String tableName = table.toString().toLowerCase();
            String schema = TxleConstants.APP_NAME;
            String txleBackupTableName = "backup_new_" + tableName;
            standbyParams.put("tablename", tableName);
            standbyParams.put("operation", "update");

            // 2.take conditions out
            SQLExpr where = deleteStatement.getWhere();
            // It doesn't matter, even though the 'where-sql' contains a line break.
            String whereSql = where.toString();
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - table: [{}] - where: [{}].", Thread.currentThread().getId(), tableName, whereSql);

            // 3.create backup table
            connection = delegate.getConnection();
            this.prepareBackupTable(connection, tableName, txleBackupTableName);

            // 4.backup data
            String backupDataSql = String.format("INSERT INTO " + schema + "." + txleBackupTableName + " SELECT *, '%s', '%s' FROM %s WHERE %s FOR UPDATE " + TxleConstants.ACTION_SQL, globalTxId, localTxId, tableName, whereSql);
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - backupDataSql: [{}].", Thread.currentThread().getId(), backupDataSql);
            int backupResult = connection.prepareStatement(backupDataSql).executeUpdate();
            return backupResult > 0;
        } catch (SQLException e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for update sql.", e);
            throw e;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for update SQL.", e);
                }
            }
        }
    }

}
