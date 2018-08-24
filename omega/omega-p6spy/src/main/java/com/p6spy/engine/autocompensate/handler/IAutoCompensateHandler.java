package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface IAutoCompensateHandler {
	
	void saveAutoCompensationInfo(PreparedStatement delegate, String executeSql, boolean isBeforeNotice) throws SQLException;

}
