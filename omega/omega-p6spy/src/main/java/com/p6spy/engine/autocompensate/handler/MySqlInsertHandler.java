package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.p6spy.engine.autocompensate.ActionConstants;

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
			String primaryKeyColumnName = this.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);
			
			// 3.take primary-key's value out
			Object primaryKeyColumnValue = getGeneratedKey(delegate);
			LOG.debug(ActionConstants.logDebugPrefixWithTime() + "The primary key info is [" + primaryKeyColumnName + " = " + primaryKeyColumnValue + "] to table [" + tableName + "].");

			// 4.save saga_undo_log
			String compensateSql = String.format("DELETE FROM %s WHERE %s = %s", tableName, primaryKeyColumnName, primaryKeyColumnValue);
			return this.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, null, server);
		} catch (SQLException e) {
			LOG.error(ActionConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for insert SQL.", e);
			throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error(ActionConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for insert SQL.", e);
				}
			}
		}
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

}
