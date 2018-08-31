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

}
