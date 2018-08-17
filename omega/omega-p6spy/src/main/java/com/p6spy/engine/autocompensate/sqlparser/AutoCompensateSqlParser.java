package com.p6spy.engine.autocompensate.sqlparser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.saga.omega.transaction.AutoCompensableCache;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;

public class AutoCompensateSqlParser implements IAutoCompensateSqlParser {
	
	private static AutoCompensateSqlParser autoCompensateSqlParser = null;
	
	public static AutoCompensateSqlParser newInstance() {
		if (autoCompensateSqlParser == null) {
			synchronized(AutoCompensateSqlParser.class) {
				if (autoCompensateSqlParser == null) {
					autoCompensateSqlParser = new AutoCompensateSqlParser();
				}				
			}
		}
		return autoCompensateSqlParser;
	}

	@Override
	public void saveAutoCompensationInfo(PreparedStatement delegate, String executeSql, boolean isBeforeNotice) throws SQLException {
		String localTxId = AutoCompensableCache.getLocalTxIdByCurThreadId();
		if (localTxId == null || localTxId.length() == 0) {
			return;
		}
		String server = "";
		
		// To parse SQL by SQLParser tools from Druid.
		MySqlStatementParser parser = new MySqlStatementParser(executeSql);
		SQLStatement sqlStatement = parser.parseStatement();
		
		if (isBeforeNotice && sqlStatement instanceof MySqlUpdateStatement) {
			AutoCompensateUpdateSqlParser.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		} else if (!isBeforeNotice && sqlStatement instanceof MySqlInsertStatement) {
			AutoCompensateInsertSqlParser.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		} else if (isBeforeNotice && sqlStatement instanceof MySqlDeleteStatement) {
			AutoCompensateDeleteSqlParser.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		}
	}
	
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		return false;
	}

	public String parsePrimaryKeyColumnName(PreparedStatement delegate, SQLStatement sqlStatement, String tableName) throws SQLException {
		String primaryKeyColumnName = "id";
		// So far, didn't know how to get primary-key from Druid. So, use the original way. 
		PreparedStatement ps = delegate.getConnection().prepareStatement("SHOW FULL COLUMNS FROM " + tableName);
		ResultSet columnResultSet = ps.executeQuery();
		while (columnResultSet.next()) {
			if ("PRI".equalsIgnoreCase(columnResultSet.getString("Key")) && primaryKeyColumnName.length() == 0) {
				primaryKeyColumnName = columnResultSet.getString("Field");
				break;
			}
		}
		return primaryKeyColumnName;
	}
	
	public Map<String, String> selectColumnNameType(PreparedStatement delegate, String tableName) throws SQLException {
		PreparedStatement prepareStatement = delegate.getConnection().prepareStatement("SHOW FULL COLUMNS FROM " + tableName);
		ResultSet resultSet = prepareStatement.executeQuery();
		Map<String, String> columnNameType = new HashMap<>();
		while (resultSet.next()) {
			columnNameType.put(resultSet.getString(1), resultSet.getString(2));// column name and type
		}
		return columnNameType;
	}
	
	/**
	 * To save auto-compensation info to data table 'saga_undo_log'.
	 * 
	 * @param preparedStatement
	 * @param connection
	 * @param localTxId
	 * @param executeSql
	 * @param compensateSql
	 * @param originalDataJson
	 * @param server
	 * @throws SQLException
	 * @author Gannalyo
	 * @since 2018-08-08
	 */
	public boolean saveSagaUndoLog(PreparedStatement delegate, String localTxId, String executeSql, String compensateSql, String originalDataJson, String server) throws SQLException {
		int index = 1;
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		PreparedStatement preparedStatement = null;
		try {
			String sql = "insert into saga_undo_log(globaltxid, localtxid, executesql, compensatesql, originalinfo, status, server, lastmodifytime, createtime) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
			preparedStatement = delegate.getConnection().prepareStatement(sql);
			preparedStatement.setString(index++, AutoCompensableCache.getGlobalTxIdByCurThreadId());
			preparedStatement.setString(index++, localTxId);
			preparedStatement.setString(index++, executeSql);
			preparedStatement.setString(index++, compensateSql);
			preparedStatement.setString(index++, originalDataJson);
			preparedStatement.setInt(index++, 0);
			preparedStatement.setString(index++, server);
			preparedStatement.setTimestamp(index++, currentTime);
			preparedStatement.setTimestamp(index++, currentTime);
			return preparedStatement.executeUpdate() > 0;
		} finally {
			if (preparedStatement != null) {
				preparedStatement.close();
			}
		}
	}
	
}
