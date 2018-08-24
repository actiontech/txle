package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

public class AutoCompensateDeleteHandler extends AutoCompensateHandler {

	private static AutoCompensateDeleteHandler autoCompensateDeleteHandler = null;

	public static AutoCompensateDeleteHandler newInstance() {
		if (autoCompensateDeleteHandler == null) {
			synchronized (AutoCompensateDeleteHandler.class) {
				if (autoCompensateDeleteHandler == null) {
					autoCompensateDeleteHandler = new AutoCompensateDeleteHandler();
				}
			}
		}
		return autoCompensateDeleteHandler;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		
		if (JdbcConstants.MYSQL.equals(sqlStatement.getDbType())) {
			return MySqlDeleteHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		}
		
		return false;
	}

}
