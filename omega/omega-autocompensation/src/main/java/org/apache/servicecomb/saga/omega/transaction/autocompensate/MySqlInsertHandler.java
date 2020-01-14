/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 插入数据逻辑
 * 1.插入前无数据需要备份，故不做处理
 * 2.执行插入数据操作
 * 3.插入后，生成的反向SQL的where条件中包含所有字段与值，如此在后续执行反向SQL时可避免脏写问题
 * 4.插入数据操作与生成补偿备份操作在同一事务中，防止脏写，即避免业务执行后，补偿备份前有其它业务对涉及数据进行更新
 * 5.后续需要补偿时，直接执行补偿SQL即可，补偿SQL举例如：【DELETE FROM txle_sample_user WHERE createtime = '2020-01-09 13:47:15.0' and balance = 2.00000 and name = 'xiongjiujiu' and id = 1006 and version = 2;】
 *
 * ps：目前仅支持普通的插入语句，暂未支持INSERT INTO table SELECT ...等复杂插入语句
 */
public class MySqlInsertHandler extends AutoCompensateInsertHandler {

    private static volatile MySqlInsertHandler mySqlInsertHandler = null;
    private static final Logger LOG = LoggerFactory.getLogger(MySqlInsertHandler.class);

    public static MySqlInsertHandler newInstance() {
        if (mySqlInsertHandler == null) {
            synchronized (MySqlInsertHandler.class) {
                if (mySqlInsertHandler == null) {
                    mySqlInsertHandler = new MySqlInsertHandler();
                }
            }
        }
        return mySqlInsertHandler;
    }

    @Override
    public boolean prepareCompensationAfterInserting(PreparedStatement delegate, SQLStatement sqlStatement,
                                            String executeSql, String globalTxId, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
        ResultSet rs = null;
        try {
            MySqlInsertStatement insertStatement = (MySqlInsertStatement) sqlStatement;
            // 1.take table's name out
            String tableName = insertStatement.getTableName().toString().toLowerCase();
            standbyParams.put("tablename", tableName);
            standbyParams.put("operation", "insert");

            // 2.take primary-key's name out
            String primaryKeyName = this.parsePrimaryKeyColumnName(delegate, tableName);

            // 3.take primary-key's value out
            Set<Object> primaryKeyValues = getGeneratedKey(delegate);
            StringBuffer ids = new StringBuffer();
            primaryKeyValues.forEach(value -> {
                if (ids.length() == 0) {
                    ids.append(value);
                } else {
                    ids.append(", " + value);
                }
            });
            standbyParams.put("ids", ids);
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "The primary keys info is [" + primaryKeyName + " = " + ids.toString() + "] to table [" + tableName + "].");

            // 4.take the new data out
            List<Map<String, Object>> newDataList = selectNewData(delegate, tableName, primaryKeyName, primaryKeyValues);

            // 5.construct compensate sql
//			String compensateSql = String.format("DELETE FROM %s WHERE %s = %s" + TxleConstants.ACTION_SQL, tableName, primaryKeyColumnName, primaryKeyColumnValue);
            String compensateSql = constructCompensateSql(delegate, tableName, newDataList);

            // start to mark duration for business sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(compensateSql, false);

            // 6.save txle_undo_log
            boolean result = this.saveTxleUndoLog(delegate, globalTxId, localTxId, executeSql, compensateSql, server);

            // end mark duration for maintaining sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

            return result;
        } catch (SQLException e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for insert SQL.", e);
            throw e;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for insert SQL.", e);
                }
            }
        }
    }

    private String constructCompensateSql(PreparedStatement delegate, String tableName, List<Map<String, Object>> newDataList) throws SQLException {
        if (newDataList == null || newDataList.isEmpty()) {
            throw new SQLException(TxleConstants.LOG_ERROR_PREFIX + "Could not get the new data when constructed the 'compensateSql' for executing insert SQL.");
        }

        Map<String, String> columnNameType = this.selectColumnNameType(delegate, tableName);
        StringBuffer compensateSqls = new StringBuffer();
        for (Map<String, Object> dataMap : newDataList) {
            this.resetColumnValueByDBType(columnNameType, dataMap);
            String whereSqlForCompensation = this.constructWhereSqlForCompensation(dataMap);

            String compensateSql = String.format("DELETE FROM %s WHERE %s" + TxleConstants.ACTION_SQL + ";", tableName, whereSqlForCompensation);
            if (compensateSqls.length() == 0) {
                compensateSqls.append(compensateSql);
            } else {
                compensateSqls.append("\n" + compensateSql);
            }
        }

        return compensateSqls.toString();
    }

    private Set<Object> getGeneratedKey(PreparedStatement preparedStatement) throws SQLException {
        Set<Object> primaryKeyValue = new HashSet<>();
        ResultSet rs = null;
        try {
            rs = preparedStatement.getGeneratedKeys();
            while (rs != null && rs.next()) {
                primaryKeyValue.add(rs.getObject(1));
            }
        } catch (Exception e) {
            LOG.error("Failed to execute method 'getGeneratedKey'.");
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return primaryKeyValue;
    }

    private List<Map<String, Object>> selectNewData(PreparedStatement delegate, String tableName, String primaryKeyName, Set<Object> primaryKeyValues) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet dataResultSet = null;
        try {
            String sql = constructNewDataSql(primaryKeyValues);
//			dataResultSet = delegate.getResultSet();// it's result is null.

            // start to mark duration for business sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

            String[] params = new String[2 + primaryKeyValues.size()];
            params[0] = tableName;
            params[1] = primaryKeyName;
            AtomicInteger index = new AtomicInteger(2);
            primaryKeyValues.forEach(value -> {
                params[index.getAndIncrement()] = String.valueOf(value);
            });

            preparedStatement = delegate.getConnection().prepareStatement(String.format(sql, params));
            dataResultSet = preparedStatement.executeQuery();

            // end mark duration for maintaining sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

            List<Map<String, Object>> newDataList = new ArrayList<>();
            while (dataResultSet.next()) {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                ResultSetMetaData metaData = dataResultSet.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String column = metaData.getColumnName(i);
                    dataMap.put(column, dataResultSet.getObject(column));
                }

                newDataList.add(dataMap);
            }
            return newDataList;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (dataResultSet != null) {
                dataResultSet.close();
            }
        }
    }

    private String constructNewDataSql(Set<Object> primaryKeyValues) {
        if (primaryKeyValues != null && !primaryKeyValues.isEmpty()) {
            StringBuffer sql = new StringBuffer("SELECT * FROM %s T WHERE T.%s IN (");
            if (primaryKeyValues.size() < 1000) {
                for (int i = 0; i < primaryKeyValues.size(); i++) {
                    if (i == 0) {
                        sql.append("%s");
                    } else {
                        sql.append(", %s");
                    }
                }
//            } else {
                // TODO Do not forget to handle a case which the value size is more 1000.
            }
            sql.append(")");
            return sql.toString();
        }
        return "";
    }

}
