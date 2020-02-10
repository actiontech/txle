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
 * update logic
 * 1.create backup table
 * 2.write original data to the backup table before updating
 * 3.perform update operation
 * 4.write new data to the backup table after updating
 * 5.in a transaction, to prepare backup data and delete original data, the aim is to prevent a dirty change
 * 6.check data's consistency between backup data and current latest data in case of error, if it's true, then perform compensation sql, if not, report to accident platform
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
            String backupDataSql = String.format("INSERT INTO " + this.schema + "." + txleBackupTableName + " SELECT *, '%s', '%s' FROM %s WHERE %s FOR UPDATE " + TxleConstants.ACTION_SQL, globalTxId, localTxId, tableName, whereSql);
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - backupDataSql: [{}].", Thread.currentThread().getId(), backupDataSql);
            int backupResult = connection.prepareStatement(backupDataSql).executeUpdate();
            if (backupResult > 0) {
                // 5.construct compensateSql
                preparedStatement = connection.prepareStatement("SELECT GROUP_CONCAT(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + this.schema + "' AND TABLE_NAME = '" + txleBackupTableName + "' AND COLUMN_NAME NOT IN ('globalTxId', 'localTxId')");
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
                            + TxleConstants.ACTION_SQL, tableName, this.schema + "." + txleBackupTableName, setColumns.toString(), globalTxId, localTxId);

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

            // take primary-key name
            String primaryKeyColumnName = this.parsePrimaryKeyColumnName(delegate, tableName);
            // 4.backup data
            // 4.1 delete the previous backup for some data, only reserve the latest backup
            String deletePreviousBackupSql = String.format("DELETE FROM " + this.schema + "." + txleBackupTableName + " WHERE globalTxId = '%s' AND localTxId = '%s'" +
                    " AND " + primaryKeyColumnName + " IN (SELECT " + primaryKeyColumnName + " FROM %s WHERE %s) " + TxleConstants.ACTION_SQL, globalTxId, localTxId, tableName, whereSql);
            connection.prepareStatement(deletePreviousBackupSql).executeUpdate();
            String backupDataSql = String.format("INSERT INTO " + this.schema + "." + txleBackupTableName + " SELECT *, '%s', '%s' FROM %s WHERE %s FOR UPDATE " + TxleConstants.ACTION_SQL, globalTxId, localTxId, tableName, whereSql);
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
