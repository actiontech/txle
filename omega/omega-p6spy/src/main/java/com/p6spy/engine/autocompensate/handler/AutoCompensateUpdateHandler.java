package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

public class AutoCompensateUpdateHandler extends AutoCompensateHandler {

	private static AutoCompensateUpdateHandler autoCompensateUpdateHandler = null;

	public static AutoCompensateUpdateHandler newInstance() {
		if (autoCompensateUpdateHandler == null) {
			synchronized (AutoCompensateUpdateHandler.class) {
				if (autoCompensateUpdateHandler == null) {
					autoCompensateUpdateHandler = new AutoCompensateUpdateHandler();
				}
			}
		}
		return autoCompensateUpdateHandler;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		
		if (JdbcConstants.MYSQL.equals(sqlStatement.getDbType())) {
			return MySqlUpdateHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
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
