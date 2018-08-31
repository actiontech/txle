package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.servicecomb.saga.omega.context.UtxConstants;
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
			String executeSql, String localTxId, String server) throws SQLException {
		ResultSet rs = null;
		try {
			MySqlInsertStatement insertStatement = (MySqlInsertStatement) sqlStatement;
			// 1.take table's name out
			String tableName = insertStatement.getTableName().toString();

			// 2.take primary-key's name out
			String primaryKeyName = this.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);

			// 3.take primary-key's value out
			Object primaryKeyValue = getGeneratedKey(delegate);
			LOG.debug(UtxConstants.logDebugPrefixWithTime() + "The primary key info is [" + primaryKeyName + " = " + primaryKeyValue + "] to table [" + tableName + "].");
			
			// 4.take the new data out
			List<Map<String, Object>> newDataList = selectNewData(delegate, tableName, primaryKeyName, primaryKeyValue);

			// 5.save saga_undo_log
//			String compensateSql = String.format("DELETE FROM %s WHERE %s = %s" + UtxConstants.ACTION_SQL, tableName, primaryKeyColumnName, primaryKeyColumnValue);
			String compensateSql = constructCompensateSql(delegate, insertStatement, tableName, newDataList);
			return this.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, null, server);
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
	
	private Object getGeneratedKey(PreparedStatement preparedStatement) throws SQLException {
		Object primaryKeyValue = null;
		ResultSet rs = null;
		try {
			rs = preparedStatement.getGeneratedKeys();
			if (rs != null && rs.next()) {
				primaryKeyValue = rs.getObject(1);
			}
		} finally {
			if (rs != null) {
				rs.close();
			}
		}
		return primaryKeyValue;
	}
	
	private List<Map<String, Object>> selectNewData(PreparedStatement delegate, String tableName, String primaryKeyName, Object primaryKeyValue) throws SQLException {
		PreparedStatement preparedStatement = null;
		ResultSet dataResultSet = null;
		try {
			String sql = String.format("SELECT * FROM %s T WHERE T.%s = %s", tableName, primaryKeyName, primaryKeyValue);
//			dataResultSet = delegate.getResultSet();// it's result is null.
			preparedStatement = delegate.getConnection().prepareStatement(sql);
			dataResultSet = preparedStatement.executeQuery();
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
	
}
