package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.p6spy.engine.monitor.UtxSqlMetrics;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;

public class MySqlInsertHandler extends AutoCompensateInsertHandler {

	private static MySqlInsertHandler mySqlInsertHandler = null;
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
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement,
											String executeSql, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
		ResultSet rs = null;
		try {
			MySqlInsertStatement insertStatement = (MySqlInsertStatement) sqlStatement;
			// 1.take table's name out
			String tableName = insertStatement.getTableName().toString();
			standbyParams.put("tablename",tableName);
			standbyParams.put("operation", "insert");

			// 2.take primary-key's name out
			String primaryKeyName = this.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);

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
			LOG.debug(UtxConstants.logDebugPrefixWithTime() + "The primary keys info is [" + primaryKeyName + " = " + ids.toString() + "] to table [" + tableName + "].");

			// 4.take the new data out
			List<Map<String, Object>> newDataList = selectNewData(delegate, tableName, primaryKeyName, primaryKeyValues);

			// 5.save utx_undo_log
//			String compensateSql = String.format("DELETE FROM %s WHERE %s = %s" + UtxConstants.ACTION_SQL, tableName, primaryKeyColumnName, primaryKeyColumnValue);
			String compensateSql = constructCompensateSql(delegate, insertStatement, tableName, newDataList);

			// start to mark duration for business sql By Gannalyo.
			UtxSqlMetrics.startMarkSQLDurationAndCount(compensateSql, false);

			boolean result = this.saveUtxUndoLog(delegate, localTxId, executeSql, compensateSql, null, server);

			// end mark duration for maintaining sql By Gannalyo.
			UtxSqlMetrics.endMarkSQLDuration();

			return  result;
		} catch (SQLException e) {
			LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for insert SQL.", e);
			throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for insert SQL.", e);
				}
			}
		}
	}
	
	private String constructCompensateSql(PreparedStatement delegate, MySqlInsertStatement insertStatement, String tableName, List<Map<String, Object>> newDataList) throws SQLException {
		if (newDataList == null || newDataList.isEmpty()) {
			throw new SQLException(UtxConstants.LOG_ERROR_PREFIX + "Could not get the new data when constructed the 'compensateSql' for executing insert SQL.");
		}
		
		Map<String, String> columnNameType = this.selectColumnNameType(delegate, tableName);
		StringBuffer compensateSqls = new StringBuffer();
		for (Map<String, Object> dataMap : newDataList) {
			this.resetColumnValueByDBType(columnNameType, dataMap);
			String whereSqlForCompensation = this.constructWhereSqlForCompensation(dataMap);
			
			String compensateSql = String.format("DELETE FROM %s WHERE %s" + UtxConstants.ACTION_SQL + ";", tableName, whereSqlForCompensation);
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
			UtxSqlMetrics.startMarkSQLDurationAndCount(sql, false);

			String[] params = new String[2 + primaryKeyValues.size()];
			params[0] = tableName;
			params[1] = primaryKeyName;
			AtomicInteger index = new AtomicInteger(2);
			primaryKeyValues.forEach(value -> {params[index.getAndIncrement()] = String.valueOf(value);});

			preparedStatement = delegate.getConnection().prepareStatement(String.format(sql, params));
			dataResultSet = preparedStatement.executeQuery();

			// end mark duration for maintaining sql By Gannalyo.
			UtxSqlMetrics.endMarkSQLDuration();

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
				for (int i = 0; i < primaryKeyValues.size(); i ++) {
					if (i == 0) {
						sql.append("%s");
					} else {
						sql.append(", %s");
					}
				}
			} else {
				// TODO Do not forget to handle a case which the value size is more 1000.
			}
			sql.append(")");
			return sql.toString();
		}
		return "";
	}

}
