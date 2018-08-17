package com.p6spy.engine.autocompensate.sqlparser;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface IAutoCompensateSqlParser {
	
	void saveAutoCompensationInfo(PreparedStatement delegate, String executeSql, boolean isBeforeNotice) throws SQLException;

}
