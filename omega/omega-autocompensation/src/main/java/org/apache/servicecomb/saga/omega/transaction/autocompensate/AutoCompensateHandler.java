/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlSelectIntoStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.TxleStaticConfig;
import org.apache.servicecomb.saga.omega.transaction.DataSourceMappingCache;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AutoCompensateHandler implements IAutoCompensateHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AutoCompensateHandler.class);

    private static volatile AutoCompensateHandler autoCompensateHandler = null;
    private final String schema = TxleConstants.APP_NAME;

    public static AutoCompensateHandler newInstance() {
        if (autoCompensateHandler == null) {
            synchronized (AutoCompensateHandler.class) {
                if (autoCompensateHandler == null) {
                    autoCompensateHandler = new AutoCompensateHandler();
                }
            }
        }
        return autoCompensateHandler;
    }

    protected String schema() {
        return schema;
    }

    @Override
    public void prepareCompensationBeforeExecuting(PreparedStatement delegate, String executeSql, Map<String, Object> standbyParams) throws SQLException {
        String globalTxId = CurrentThreadOmegaContext.getGlobalTxIdFromCurThread();
        if (globalTxId == null || globalTxId.length() == 0) {
            return;
        }
        String localTxId = CurrentThreadOmegaContext.getLocalTxIdFromCurThread();
        if (localTxId == null || localTxId.length() == 0) {
            return;
        }

        // To parse SQL by SQLParser tools from Druid.
        MySqlStatementParser parser = new MySqlStatementParser(executeSql);
        SQLStatement sqlStatement = parser.parseStatement();
        if (sqlStatement instanceof MySqlSelectIntoStatement) {
            return;
        }

        if (standbyParams == null) {
            standbyParams = new HashMap<>();
        }

        String server = CurrentThreadOmegaContext.getServiceNameFromCurThread();

        // To set a relationship between localTxId and datSourceInfo, in order to determine to use the relative dataSource for localTxId when it need be compensated.
        DatabaseMetaData databaseMetaData = delegate.getConnection().getMetaData();
        String dburl = databaseMetaData.getURL(), dbusername = databaseMetaData.getUserName(), dbdrivername = databaseMetaData.getDriverName();
        DataSourceMappingCache.putLocalTxIdAndDataSourceInfo(localTxId, dburl, dbusername, dbdrivername);
        // To construct kafka message.
        standbyParams.put("dbdrivername", dbdrivername);
        standbyParams.put("dburl", dburl);
        standbyParams.put("dbusername", dbusername);

        if (sqlStatement instanceof MySqlInsertStatement) {
            return;
        } else if (sqlStatement instanceof MySqlUpdateStatement) {
            AutoCompensateUpdateHandler.newInstance().prepareCompensationBeforeUpdating(delegate, sqlStatement, executeSql, globalTxId, localTxId, server, standbyParams);
        } else if (sqlStatement instanceof MySqlDeleteStatement) {
            AutoCompensateDeleteHandler.newInstance().prepareCompensationBeforeDeleting(delegate, sqlStatement, executeSql, globalTxId, localTxId, server, standbyParams);
        } else {
            standbyParams.clear();
            // Default is closed, means that just does record, if it's open, then program will throw an exception about current special SQL, just for auto-compensation.
            boolean checkSpecialSql = TxleStaticConfig.getBooleanConfig("txle.transaction.auto-compensation.check-special-sql", false);
            if (checkSpecialSql) {
                throw new SQLException(TxleConstants.logErrorPrefixWithTime() + "Do not support sql [" + executeSql + "] to auto-compensation.");
            } else {
                LOG.debug(TxleConstants.logDebugPrefixWithTime() + "Do not support sql [{}] to auto-compensation, but it has been executed due to the switch 'checkSpecialSql' is closed.", executeSql);
            }
        }
    }

    @Override
    public void prepareCompensationAfterExecuting(PreparedStatement delegate, String executeSql, Map<String, Object> standbyParams) throws SQLException {
        String globalTxId = CurrentThreadOmegaContext.getGlobalTxIdFromCurThread();
        if (globalTxId == null || globalTxId.length() == 0) {
            return;
        }
        String localTxId = CurrentThreadOmegaContext.getLocalTxIdFromCurThread();
        if (localTxId == null || localTxId.length() == 0) {
            return;
        }

        // To parse SQL by SQLParser tools from Druid.
        MySqlStatementParser parser = new MySqlStatementParser(executeSql);
        SQLStatement sqlStatement = parser.parseStatement();
        if (sqlStatement instanceof MySqlSelectIntoStatement) {
            return;
        }

        if (standbyParams == null) {
            standbyParams = new HashMap<>();
        }

        String server = CurrentThreadOmegaContext.getServiceNameFromCurThread();

        // To set a relationship between localTxId and datSourceInfo, in order to determine to use the relative dataSource for localTxId when it need be compensated.
        DatabaseMetaData databaseMetaData = delegate.getConnection().getMetaData();
        String dburl = databaseMetaData.getURL(), dbusername = databaseMetaData.getUserName(), dbdrivername = databaseMetaData.getDriverName();
        DataSourceMappingCache.putLocalTxIdAndDataSourceInfo(localTxId, dburl, dbusername, dbdrivername);
        // To construct kafka message.
        standbyParams.put("dbdrivername", dbdrivername);
        standbyParams.put("dburl", dburl);
        standbyParams.put("dbusername", dbusername);

        if (sqlStatement instanceof MySqlInsertStatement) {
            AutoCompensateInsertHandler.newInstance().prepareCompensationAfterInserting(delegate, sqlStatement, executeSql, globalTxId, localTxId, server, standbyParams);
        } else if (sqlStatement instanceof MySqlUpdateStatement) {
            AutoCompensateUpdateHandler.newInstance().prepareCompensationAfterUpdating(delegate, sqlStatement, executeSql, globalTxId, localTxId, server, standbyParams);
        }
    }

    protected Map<String, String> selectColumnNameType(PreparedStatement delegate, String tableName) throws SQLException {
        String sql = "SHOW FULL COLUMNS FROM " + tableName + TxleConstants.ACTION_SQL;

        // start to mark duration for maintaining sql By Gannalyo.
        ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

        PreparedStatement ps = null;
        ResultSet columnResultSet = null;
        Map<String, String> columnNameType = new HashMap<>();
        try {
            ps = delegate.getConnection().prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();

            // end mark duration for maintaining sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

            while (resultSet.next()) {
                // column name and type
                columnNameType.put(resultSet.getString(1), resultSet.getString(2));
            }
            ps.close();
        } catch (SQLException e) {
            throw e;
        } catch (BeansException e) {
            throw e;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } finally {
                if (columnResultSet != null) {
                    columnResultSet.close();
                }
            }
        }
        return columnNameType;
    }

    protected void prepareBackupTable(Connection connection, String tableName, String txleBackupTableName) {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            boolean isExistsBackupTable = false;
            preparedStatement = connection.prepareStatement("SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + schema + "' AND TABLE_NAME = '" + txleBackupTableName + "'");
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                isExistsBackupTable = resultSet.getInt(1) > 0;
            }
            if (!isExistsBackupTable) {
                connection.prepareStatement("CREATE DATABASE IF NOT EXISTS txle DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci").execute();
                // copy table without constraints(pk, index...) so that the original data could be written for many times.
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + schema + "." + txleBackupTableName + " AS SELECT * FROM " + tableName + " LIMIT 0").execute();
                connection.prepareStatement("ALTER TABLE " + schema + "." + txleBackupTableName + " ADD globalTxId VARCHAR(36)").execute();
                connection.prepareStatement("ALTER TABLE " + schema + "." + txleBackupTableName + " ADD localTxId VARCHAR(36)").execute();
            }
        } catch (SQLException e) {
            // No obviously effect to main business in case of error.
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to create backup table for txle.", e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to close PreparedStatement after executing method 'saveAutoCompensationInfo' for delete SQL.", e);
                }
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

    protected String parsePrimaryKeyColumnName(PreparedStatement delegate, String tableName) throws SQLException {
        String primaryKeyColumnName = "id", sql = "SHOW FULL COLUMNS FROM " + tableName + TxleConstants.ACTION_SQL;

        // start to mark duration for maintaining sql By Gannalyo.
        ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

        // So far, didn't know how to get primary-key from Druid. So, use the original way.
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet columnResultSet = null;
        try {
            connection = delegate.getConnection();
            ps = connection.prepareStatement(sql);
            columnResultSet = ps.executeQuery();

            // end mark duration for maintaining sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

            while (columnResultSet.next()) {
                if ("PRI".equalsIgnoreCase(columnResultSet.getString("Key")) && primaryKeyColumnName.length() == 0) {
                    primaryKeyColumnName = columnResultSet.getString("Field");
                    break;
                }
            }
        } catch (SQLException e) {
            throw e;
        } catch (BeansException e) {
            throw e;
        } finally {
//            try {
//                if (connection != null) connection.close();
//            } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } finally {
                if (columnResultSet != null) {
                    columnResultSet.close();
                }
            }
//            }
        }
        return primaryKeyColumnName;
    }

    protected void resetColumnValueByDBType(Map<String, String> columnNameType, Map<String, Object> dataMap) {
        Iterator<Map.Entry<String, Object>> iterator = dataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            // TODO support all of column type.
            String key = entry.getKey();
            String type = columnNameType.get(key);
            if (type == null && key.startsWith("n_c_v_")) {
                type = columnNameType.get(key.substring(6));
            }
            if (type != null && (type.startsWith("varchar") || "datetime".equalsIgnoreCase(type))) {
                dataMap.put(key, "'" + entry.getValue() + "'");
            }
        }
    }

    protected String constructWhereSqlForCompensation(Map<String, Object> dataMap) throws SQLException {
        StringBuffer whereSqlForCompensation = new StringBuffer();
        Iterator<Map.Entry<String, Object>> iterator = dataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            if (!key.startsWith("n_c_v_")) {
                Object value = entry.getValue();
//				if (value == null) {
                if (dataMap.keySet().contains("n_c_v_" + key)) {
                    value = dataMap.get("n_c_v_" + key);
                }
                if (whereSqlForCompensation.length() == 0) {
                    whereSqlForCompensation.append(key + " = " + value);
                } else {
                    whereSqlForCompensation.append(" and " + key + " = " + value);
                }
            }
        }
        return whereSqlForCompensation.toString();
    }

    public boolean saveTxleUndoLog(PreparedStatement delegate, String globalTxId, String localTxId, String executeSql, String compensateSql, String server) throws SQLException {
        int index = 1;
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        PreparedStatement preparedStatement = null;
        try {
            String sql = "insert into txle_undo_log(globaltxid, localtxid, executesql, compensatesql, status, server, lastmodifytime, createtime) values (?, ?, ?, ?, ?, ?, ?, ?)" + TxleConstants.ACTION_SQL;
            preparedStatement = delegate.getConnection().prepareStatement(sql);
            preparedStatement.setString(index++, globalTxId);
            preparedStatement.setString(index++, localTxId);
            preparedStatement.setString(index++, executeSql);
            preparedStatement.setString(index++, compensateSql);
            preparedStatement.setInt(index++, 0);
            preparedStatement.setString(index++, server);
            preparedStatement.setTimestamp(index++, currentTime);
            preparedStatement.setTimestamp(index++, currentTime);

            // start to mark duration for maintaining sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

            boolean result = preparedStatement.executeUpdate() > 0;

            // end mark duration for maintaining sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

            return result;
        } catch (Exception e) {
            LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to save undo_log, localTxId=[{}].", localTxId, e);
            return false;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

}
