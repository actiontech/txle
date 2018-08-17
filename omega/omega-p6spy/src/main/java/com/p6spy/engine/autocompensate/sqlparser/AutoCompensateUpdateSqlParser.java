package com.p6spy.engine.autocompensate.sqlparser;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

public class AutoCompensateUpdateSqlParser extends AutoCompensateSqlParser {

	private static AutoCompensateUpdateSqlParser autoCompensateUpdateSqlParser = null;

	public static AutoCompensateUpdateSqlParser newInstance() {
		if (autoCompensateUpdateSqlParser == null) {
			synchronized (AutoCompensateUpdateSqlParser.class) {
				if (autoCompensateUpdateSqlParser == null) {
					autoCompensateUpdateSqlParser = new AutoCompensateUpdateSqlParser();
				}
			}
		}
		return autoCompensateUpdateSqlParser;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		
		if (JdbcConstants.MYSQL.equals(sqlStatement.getDbType())) {
			return MySqlUpdateSqlParser.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		}
		
		return false;
	}
	
	@Override
	public String parsePrimaryKeyColumnName(PreparedStatement delegate, SQLStatement sqlStatement,
			String tableName) throws SQLException {
		return super.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);
	}
	
	@Override
	public boolean saveSagaUndoLog(PreparedStatement delegate, String localTxId, String executeSql,
			String compensateSql, String originalDataJson, String server) throws SQLException {
		return super.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, originalDataJson, server);
	}

}
