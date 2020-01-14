/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * 删除数据逻辑
 * 1.删除前备份影响数据到对应的备份数据表
 * 2.执行删除数据操作
 * 3.删除后，数据已不存在，故不做处理
 * 4.删除数据操作与生成补偿备份操作在同一事务中，防止脏写，即补偿备份后，删除数据前有其它业务对涉及数据进行更新
 * 5.后续需要补偿时，直接执行补偿SQL，补偿SQL举例如：【INSERT INTO txle_sample_user SELECT id,name,balance,version,createtime FROM txle.backup_txle_sample_user WHERE globalTxId = '0457a9fd-5203-42c5-b5dc-817e4028d07e' AND localTxId = '6ff9679c-409c-4de5-b800-f3da04b581b6' FOR UPDATE】
 */
public class MySqlDeleteHandler extends AutoCompensateDeleteHandler {

    private static volatile MySqlDeleteHandler mySqlDeleteHandler = null;
    private static final Logger LOG = LoggerFactory.getLogger(MySqlDeleteHandler.class);
    private String schema = TxleConstants.APP_NAME;

    public static MySqlDeleteHandler newInstance() {
        if (mySqlDeleteHandler == null) {
            synchronized (MySqlDeleteHandler.class) {
                if (mySqlDeleteHandler == null) {
                    mySqlDeleteHandler = new MySqlDeleteHandler();
                }
            }
        }
        return mySqlDeleteHandler;
    }

    @Override
    public boolean prepareCompensationBeforeDeleting(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String globalTxId, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
        Connection connection;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            MySqlDeleteStatement deleteStatement = (MySqlDeleteStatement) sqlStatement;
            // 1.take table's name out
            String tableName = deleteStatement.getTableName().toString().toLowerCase();
            String txleBackupTableName = "backup_old_" + tableName;
            standbyParams.put("tablename", tableName);
            standbyParams.put("operation", "delete");

            // 2.take conditions out
            SQLExpr where = deleteStatement.getWhere();
            // It doesn't matter, even though the 'where-sql' contains prepareBackupTable line break.
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
                    String fieldNames = resultSet.getString(1);
                    String compensateSql = String.format("INSERT INTO " + tableName + " SELECT " + fieldNames + " FROM %s WHERE globalTxId = '%s' AND localTxId = '%s' FOR UPDATE " + TxleConstants.ACTION_SQL, schema + "." + txleBackupTableName, globalTxId, localTxId);

                    // 6.save txle_undo_log
                    return this.saveTxleUndoLog(delegate, globalTxId, localTxId, executeSql, compensateSql, server);
                }
            }
            return false;
        } catch (SQLException e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for delete sql.", e);
            throw e;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for delete SQL.", e);
                }
            }
        }
    }

}
