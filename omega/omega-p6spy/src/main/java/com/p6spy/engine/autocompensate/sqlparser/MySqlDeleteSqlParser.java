package com.p6spy.engine.autocompensate.sqlparser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.fastjson.JSON;
import com.p6spy.engine.autocompensate.AutoCompensableConstants;

public class MySqlDeleteSqlParser extends AutoCompensateDeleteSqlParser {

	private static MySqlDeleteSqlParser mySqlDeleteSqlParser = null;
	private static final Logger LOG = LoggerFactory.getLogger(MySqlDeleteSqlParser.class);

	public static MySqlDeleteSqlParser newInstance() {
		if (mySqlDeleteSqlParser == null) {
			synchronized (MySqlDeleteSqlParser.class) {
				if (mySqlDeleteSqlParser == null) {
					mySqlDeleteSqlParser = new MySqlDeleteSqlParser();
				}
			}
		}
		return mySqlDeleteSqlParser;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		ResultSet rs = null;
		try {
			// 1.delegate.execute("SHOW WARNINGS");// Cause SQLException for business interface while there is an auto-increment primary key in business SQL. 
			PreparedStatement preparedStatement = delegate.getConnection().prepareStatement("SHOW WARNINGS");
			preparedStatement.executeQuery();
			preparedStatement.close();
			
			MySqlDeleteStatement deleteStatement = (MySqlDeleteStatement) sqlStatement;
			// 2.take table's name out
			SQLName table = deleteStatement.getTableName();
			String tableName = table.toString();

			// 3.take conditions out
			SQLExpr where = deleteStatement.getWhere();// select * ... by where ... ps: having, etc.
			String whereSql = where.toString().replace("\n", " ");
			LOG.debug(AutoCompensableConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - table: [{}] - where: [{}].", Thread.currentThread().getId(), tableName, whereSql);

			// 4.take primary-key name
			String primaryKeyColumnName = super.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);
			
			// 5.take the original data out and put the lock on data.
			List<Map<String, Object>> originalData = selectOriginalData(delegate, deleteStatement, tableName, primaryKeyColumnName, whereSql);
			if (originalData == null || originalData.isEmpty()) {
				LOG.debug(AutoCompensableConstants.logDebugPrefixWithTime() + "Did not save compensation info to table 'Saga_Undo_Log' due to the executeSql's result hadn't any effect to data. localTxId: [{}], server: [{}].", localTxId, server);
				return true;
			}
			String originalDataJson = JSON.toJSONString(originalData);

			// PS: Do not joint the conditions of 'executeSql' to 'compensateSql' immediately, because the result may not be same to execute a SQL twice at different time.
			// Recommendation: construct compensateSql by original data.
			// 6.construct compensateSql
			String compensateSql = constructCompensateSql(delegate, deleteStatement, tableName, primaryKeyColumnName, originalData);
			
			// 7.save saga_undo_log
			return super.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, originalDataJson, server);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String constructCompensateSql(PreparedStatement delegate, MySqlDeleteStatement deleteStatement, String tableName, String primaryKeyColumnName, List<Map<String, Object>> originalData) throws SQLException {
		Map<String, String> columnNameType = super.selectColumnNameType(delegate, tableName);
		StringBuffer compensateSqls = new StringBuffer();
		originalData.forEach(dataMap -> {
			StringBuffer columns = new StringBuffer();
			StringBuffer placeholder = new StringBuffer();
			dataMap.keySet().forEach(key -> {
				if (columns.length() == 0) {
					columns.append(key);
					placeholder.append("%s");
				} else {
					columns.append("," + key);
					placeholder.append(",%s");
				}
				// TODO Some special data type will be handled later, such as blob, binary, and the like.
				if ("datetime".equalsIgnoreCase(columnNameType.get(key))) {
					dataMap.put(key, "'" + dataMap.get(key) + "'");
				}
			});
			
			String compensateSql = String.format("INSERT IGNORE INTO " + tableName + " (" + columns + ") VALUES (" + placeholder + ");", dataMap.values().toArray());
			if (compensateSqls.length() == 0) {
				compensateSqls.append(compensateSql);
			} else {
				compensateSqls.append("\n" + compensateSql);
			}
		});
		
		return compensateSqls.toString();
	}

	private List<Map<String, Object>> selectOriginalData(PreparedStatement delegate, MySqlDeleteStatement deleteStatement, String tableName, String primaryKeyColumnName, String whereSql) throws SQLException {
		PreparedStatement preparedStatement = null;
		try {
			String originalDataSql = String.format("SELECT * FROM %s WHERE %s FOR UPDATE", tableName, whereSql);// 'FOR UPDATE' is needed to lock data.
			LOG.debug(AutoCompensableConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - originalDataSql: [{}].", Thread.currentThread().getId(), originalDataSql);
			
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
			preparedStatement.close();
		}
	}

}
