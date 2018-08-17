package com.p6spy.engine.autocompensate.sqlparser;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

public class AutoCompensateInsertSqlParser extends AutoCompensateSqlParser {

	private static AutoCompensateInsertSqlParser autoCompensateInsertSqlParser = null;

	public static AutoCompensateInsertSqlParser newInstance() {
		if (autoCompensateInsertSqlParser == null) {
			synchronized (AutoCompensateInsertSqlParser.class) {
				if (autoCompensateInsertSqlParser == null) {
					autoCompensateInsertSqlParser = new AutoCompensateInsertSqlParser();
				}
			}
		}
		return autoCompensateInsertSqlParser;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		
		if (JdbcConstants.MYSQL.equals(sqlStatement.getDbType())) {
			return MySqlInsertSqlParser.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		}
		
		return false;
	}

}
