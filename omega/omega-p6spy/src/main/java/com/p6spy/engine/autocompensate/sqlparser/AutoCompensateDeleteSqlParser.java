package com.p6spy.engine.autocompensate.sqlparser;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

public class AutoCompensateDeleteSqlParser extends AutoCompensateSqlParser {

	private static AutoCompensateDeleteSqlParser autoCompensateDeleteSqlParser = null;

	public static AutoCompensateDeleteSqlParser newInstance() {
		if (autoCompensateDeleteSqlParser == null) {
			synchronized (AutoCompensateDeleteSqlParser.class) {
				if (autoCompensateDeleteSqlParser == null) {
					autoCompensateDeleteSqlParser = new AutoCompensateDeleteSqlParser();
				}
			}
		}
		return autoCompensateDeleteSqlParser;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		
		if (JdbcConstants.MYSQL.equals(sqlStatement.getDbType())) {
			return MySqlDeleteSqlParser.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		}
		
		return false;
	}

}
