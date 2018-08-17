package com.p6spy.engine.autocompensate.sqlparser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.p6spy.engine.autocompensate.AutoCompensableConstants;

public class MySqlInsertSqlParser extends AutoCompensateInsertSqlParser {

	private static MySqlInsertSqlParser mySqlInsertSqlParser = null;
	private static final Logger LOG = LoggerFactory.getLogger(MySqlInsertSqlParser.class);

	public static MySqlInsertSqlParser newInstance() {
		if (mySqlInsertSqlParser == null) {
			synchronized (MySqlInsertSqlParser.class) {
				if (mySqlInsertSqlParser == null) {
					mySqlInsertSqlParser = new MySqlInsertSqlParser();
				}
			}
		}
		return mySqlInsertSqlParser;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement,
			String executeSql, String localTxId, String server) throws SQLException {
		ResultSet rs = null;
		try {
			// 1.delegate.execute("SHOW WARNINGS");// Cause SQLException for business interface while there is an auto-increment primary key in business SQL. 
			PreparedStatement preparedStatement = delegate.getConnection().prepareStatement("SHOW WARNINGS");
			preparedStatement.executeQuery();
			preparedStatement.close();
			
			MySqlInsertStatement insertStatement = (MySqlInsertStatement) sqlStatement;
			// 2.take table's name out
			String tableName = insertStatement.getTableName().toString();

			// 3.take primary-key's name out
			String primaryKeyColumnName = super.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);
			
			// 4.take primary-key's value out
			Object primaryKeyColumnValue = getGeneratedKey(delegate);
			LOG.debug(AutoCompensableConstants.logDebugPrefixWithTime() + "The primary key info is [" + primaryKeyColumnName + " = " + primaryKeyColumnValue + "] to table [" + tableName + "].");

			// 5.save saga_undo_log
			String compensateSql = String.format("DELETE FROM %s WHERE %s = %s", tableName, primaryKeyColumnName, primaryKeyColumnValue);
			return super.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, null, server);
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
