/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class AutoCompensateDeleteHandler extends AutoCompensateHandler {

	private static volatile AutoCompensateDeleteHandler autoCompensateDeleteHandler = null;

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
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {

		if (JdbcConstants.MYSQL.equals(sqlStatement.getDbType())) {
			return MySqlDeleteHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server, standbyParams);
		}

		return false;
	}

}
