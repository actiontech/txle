/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.fastjson.JSON;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

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
    public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
        ResultSet rs = null;
        try {
            MySqlUpdateStatement updateStatement = (MySqlUpdateStatement) sqlStatement;
            // 1.take table's name out
            SQLName table = updateStatement.getTableName();
            String tableName = table.toString();
            standbyParams.put("tablename", tableName);
            standbyParams.put("operation", "update");

            // 2.take conditions out
            // select * ... by where ... ps: having, etc.
            SQLExpr where = updateStatement.getWhere();
            // It doesn't matter, even though the 'where-sql' contains a line break.
            String whereSql = where.toString();
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - table: [{}] - where: [{}].", Thread.currentThread().getId(), tableName, whereSql);

            // 3.take primary-key name
            String primaryKeyColumnName = this.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);

            // 4.take the original data out and put a lock on data.
            /**
             * logic here.
             * 1.For compensation SQL, append the latest value of all fields to the 'WHERE' conditions. The aim is to detect whether the data is modified when compensation SQL is executed later.
             *      Report exception information to the Accident Platform in case of compensating abortively.
             * 2.Read the latest value (after modifying) instead of using the 'WHERE' conditions directly, because conditions maybe contain some formulas, such as 'money = money - 10'.
             * 3.Read old and new data.
             * 4.Separate old and new data.
             */
            List<Map<String, Object>> newDataList = selectNewDataList(delegate, updateStatement, tableName, primaryKeyColumnName, whereSql);
            List<Map<String, Object>> originalDataList = selectOriginalData(newDataList);
            if (originalDataList == null || originalDataList.isEmpty()) {
                LOG.debug(TxleConstants.logDebugPrefixWithTime() + "Did not save compensation info to table 'Txle_Undo_Log' due to the executeSql's result hadn't any effect to data. localTxId: [{}], server: [{}].", localTxId, server);
                return true;
            }
            StringBuffer ids = new StringBuffer();
            originalDataList.forEach(map -> {
                if (ids.length() == 0) {
                    ids.append(map.get(primaryKeyColumnName));
                } else {
                    ids.append(", " + map.get(primaryKeyColumnName));
                }
            });
            standbyParams.put("ids", ids);

            String originalDataJson = JSON.toJSONString(originalDataList);

            // PS: Do not joint the conditions of 'executeSql' to 'compensateSql' immediately, because the result may not be same to execute a SQL twice at different time.
            // Recommendation: construct compensateSql by original data.
            // 5.construct compensateSql
            String compensateSql = constructCompensateSql(delegate, updateStatement, tableName, newDataList, whereSql);

            // 6.save txle_undo_log
            return this.saveTxleUndoLog(delegate, localTxId, executeSql, compensateSql, originalDataJson, server);
        } catch (SQLException e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for update SQL.", e);
            throw e;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error(TxleConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for update SQL.", e);
                }
            }
        }
    }

    private String constructCompensateSql(PreparedStatement delegate, MySqlUpdateStatement updateStatement, String tableName, List<Map<String, Object>> newDataList, String whereSql) throws SQLException {
        if (newDataList == null || newDataList.isEmpty()) {
            throw new SQLException(TxleConstants.LOG_ERROR_PREFIX + "Could not get the original data when constructed the 'compensateSql' for executing update SQL.");
        }

        List<SQLUpdateSetItem> updateSetItemList = updateStatement.getItems();
        if (updateSetItemList == null || updateSetItemList.isEmpty()) {
            throw new SQLException(TxleConstants.LOG_ERROR_PREFIX + "Had no set-item for update SQL.");
        }

        Map<String, String> columnNameType = this.selectColumnNameType(delegate, tableName);
        StringBuffer compensateSqls = new StringBuffer();
        for (Map<String, Object> dataMap : newDataList) {
            this.resetColumnValueByDBType(columnNameType, dataMap);
            String setColumns = constructSetColumns(updateSetItemList, dataMap);
            String whereSqlForCompensation = this.constructWhereSqlForCompensation(dataMap);

            String compensateSql = String.format("UPDATE %s SET %s WHERE %s" + TxleConstants.ACTION_SQL + ";", tableName, setColumns, whereSqlForCompensation);
            if (compensateSqls.length() == 0) {
                compensateSqls.append(compensateSql);
            } else {
                compensateSqls.append("\n" + compensateSql);
            }
        }

        return compensateSqls.toString();
    }

    private String constructSetColumns(List<SQLUpdateSetItem> updateSetItemList, Map<String, Object> dataMap) {
        StringBuffer setColumns = new StringBuffer();
        for (SQLUpdateSetItem setItem : updateSetItemList) {
            String column = setItem.getColumn().toString();
            if (setColumns.length() == 0) {
                setColumns.append(column + " = " + dataMap.get(column));
            } else {
                setColumns.append(", " + column + " = " + dataMap.get(column));
            }
        }
        return setColumns.toString();
    }

    private List<Map<String, Object>> selectNewDataList(PreparedStatement delegate, MySqlUpdateStatement updateStatement, String tableName, String primaryKeyColumnName, String whereSql) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            List<SQLUpdateSetItem> updateSetItemList = updateStatement.getItems();
            if (updateSetItemList == null || updateSetItemList.isEmpty()) {
                throw new SQLException("Have no set-item for update SQL.");
            }

            StringBuffer newColumnValues = new StringBuffer();
            for (SQLUpdateSetItem setItem : updateSetItemList) {
                String columnValue = setItem.getValue().toString();
                if (newColumnValues.length() > 0) {
                    newColumnValues.append(", ");
                }
                newColumnValues.append(columnValue + " n_c_v_" + setItem.getColumn().toString());
            }

            // 'FOR UPDATE' is needed to lock data.
            String originalDataSql = String.format("SELECT T.*, %s FROM %s T WHERE %s FOR UPDATE" + TxleConstants.ACTION_SQL, newColumnValues, tableName, whereSql);
            LOG.debug(TxleConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - originalDataSql: [{}].", Thread.currentThread().getId(), originalDataSql);

            // start to mark duration for business sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(originalDataSql, false);

            connection = delegate.getConnection();
            preparedStatement = connection.prepareStatement(originalDataSql);
            List<Map<String, Object>> originalDataList = new ArrayList<Map<String, Object>>();
            ResultSet dataResultSet = preparedStatement.executeQuery();

            // end mark duration for maintaining sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

            while (dataResultSet.next()) {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                ResultSetMetaData metaData = dataResultSet.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String column = metaData.getColumnName(i);
                    dataMap.put(column, dataResultSet.getObject(column));
                }

                originalDataList.add(dataMap);
            }
            return originalDataList;
        } finally {
//			try {
//				if (connection != null) {
//					connection.close();
//				}
//			} finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
//			}
        }
    }

    private List<Map<String, Object>> selectOriginalData(List<Map<String, Object>> newDataList) {
        List<Map<String, Object>> originalDataList = new ArrayList<>();
        for (Map<String, Object> newDataMap : newDataList) {
            Map<String, Object> originalDataMap = new HashMap<>();
            Iterator<Map.Entry<String, Object>> iterator = newDataMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                if (!entry.getKey().startsWith("n_c_v_")) {
                    originalDataMap.put(entry.getKey(), newDataMap.get(entry.getValue()));
                }
            }
            originalDataList.add(originalDataMap);
        }
        return originalDataList;
    }

}
