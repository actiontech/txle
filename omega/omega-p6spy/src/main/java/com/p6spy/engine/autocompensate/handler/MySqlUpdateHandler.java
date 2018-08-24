package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.fastjson.JSON;
import com.p6spy.engine.autocompensate.ActionConstants;

public class MySqlUpdateHandler extends AutoCompensateUpdateHandler {

	private static MySqlUpdateHandler mySqlUpdateHandler = null;
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
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		ResultSet rs = null;
		try {
			MySqlUpdateStatement updateStatement = (MySqlUpdateStatement) sqlStatement;
			// 1.take table's name out
			SQLName table = updateStatement.getTableName();
			String tableName = table.toString();

			// 2.take conditions out
			SQLExpr where = updateStatement.getWhere();// select * ... by where ... ps: having, etc.
			String whereSql = where.toString();// It doesn't matter, even though the 'where-sql' contains a line break.
			LOG.debug(ActionConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - table: [{}] - where: [{}].", Thread.currentThread().getId(), tableName, whereSql);

			// 3.take primary-key name
			String primaryKeyColumnName = this.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);
			
			// 4.take the original data out and put the lock on data.
			List<Map<String, Object>> originalData = selectOriginalData(delegate, updateStatement, tableName, primaryKeyColumnName, whereSql);
			if (originalData == null || originalData.isEmpty()) {
				LOG.debug(ActionConstants.logDebugPrefixWithTime() + "Did not save compensation info to table 'Saga_Undo_Log' due to the executeSql's result hadn't any effect to data. localTxId: [{}], server: [{}].", localTxId, server);
				return true;
			}
			String originalDataJson = JSON.toJSONString(originalData);

			// PS: Do not joint the conditions of 'executeSql' to 'compensateSql' immediately, because the result may not be same to execute a SQL twice at different time.
			// Recommendation: construct compensateSql by original data.
			// 5.construct compensateSql
			String compensateSql = constructCompensateSql(delegate, updateStatement, tableName, primaryKeyColumnName, originalData);
			
			// 6.save saga_undo_log
			return this.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, originalDataJson, server);
		} catch (SQLException e) {
			LOG.error(ActionConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for update SQL.", e);
			throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error(ActionConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for update SQL.", e);
				}
			}
		}
	}

	private String constructCompensateSql(PreparedStatement delegate, MySqlUpdateStatement updateStatement, String tableName, String primaryKeyColumnName, List<Map<String, Object>> originalData) throws SQLException {
		if (originalData == null || originalData.isEmpty()) {
			throw new SQLException(ActionConstants.LOG_ERROR_PREFIX + "Could not get the original data when constructed the 'compensateSql' for executing update SQL.");
		}

		List<SQLUpdateSetItem> updateSetItemList = updateStatement.getItems();
		if (updateSetItemList == null || updateSetItemList.isEmpty()) {
			throw new SQLException(ActionConstants.LOG_ERROR_PREFIX + "Have no set-item for update SQL.");
		}
		
		Map<String, String> columnNameType = this.selectColumnNameType(delegate, tableName);
		StringBuffer compensateSqls = new StringBuffer();
		originalData.forEach(dataMap -> {
			StringBuffer setColumns = new StringBuffer();
			for (SQLUpdateSetItem setItem : updateSetItemList) {
				String column = setItem.getColumn().toString();
				
				if (columnNameType.get(column).startsWith("varchar") || "datetime".equalsIgnoreCase(columnNameType.get(column))) {
					dataMap.put(column, "'" + dataMap.get(column) + "'");
				}

				if (setColumns.length() == 0) {
					setColumns.append(column + " = " + dataMap.get(column));
				} else {
					setColumns.append("," + column + " = " + dataMap.get(column));
				}
			}
			
			String compensateSql = String.format("UPDATE %s SET %s WHERE %s = %s;", tableName, setColumns, primaryKeyColumnName, dataMap.get(primaryKeyColumnName));
			if (compensateSqls.length() == 0) {
				compensateSqls.append(compensateSql);
			} else {
				compensateSqls.append("\n" + compensateSql);
			}
		});
		
		return compensateSqls.toString();
	}

	private List<Map<String, Object>> selectOriginalData(PreparedStatement delegate, MySqlUpdateStatement updateStatement, String tableName, String primaryKeyColumnName, String whereSql) throws SQLException {
		PreparedStatement preparedStatement = null;
		try {
			List<SQLUpdateSetItem> updateSetItemList = updateStatement.getItems();
			if (updateSetItemList == null || updateSetItemList.isEmpty()) {
				throw new SQLException("Have no set-item for update SQL.");
			}
			Set<String> columnSet = new HashSet<>();
			StringBuffer setColumns = new StringBuffer(primaryKeyColumnName);
			for (SQLUpdateSetItem setItem : updateSetItemList) {
				String column = setItem.getColumn().toString();
				setColumns.append("," + column);
				columnSet.add(column);
			}
			
			String originalDataSql = String.format("SELECT %s FROM %s WHERE %s FOR UPDATE", setColumns, tableName, whereSql);// 'FOR UPDATE' is needed to lock data.
			LOG.debug(ActionConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - originalDataSql: [{}].", Thread.currentThread().getId(), originalDataSql);
			
			preparedStatement = delegate.getConnection().prepareStatement(originalDataSql);
			List<Map<String, Object>> originalDataList = new ArrayList<Map<String, Object>>();
			ResultSet dataResultSet = preparedStatement.executeQuery();
			while (dataResultSet.next()) {
				Map<String, Object> dataMap = new HashMap<String, Object>();
				ResultSetMetaData metaData = dataResultSet.getMetaData();
				for (int i = 1; i <= metaData.getColumnCount(); i ++) {
					String column = metaData.getColumnName(i);
					dataMap.put(column, dataResultSet.getObject(column));
				}
				
				originalDataList.add(dataMap);
			}
			return originalDataList;
		} finally {
			if (preparedStatement != null) {
				preparedStatement.close();
			}
		}
	}

}
