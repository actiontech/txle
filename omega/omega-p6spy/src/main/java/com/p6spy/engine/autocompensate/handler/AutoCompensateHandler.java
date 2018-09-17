package com.p6spy.engine.autocompensate.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.saga.omega.context.UtxConstants;
import org.apache.servicecomb.saga.omega.transaction.AutoCompensableCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;

public class AutoCompensateHandler implements IAutoCompensateHandler {
	private static final Logger LOG = LoggerFactory.getLogger(AutoCompensateHandler.class);
	
	private static AutoCompensateHandler autoCompensateHandler = null;
	
	public static AutoCompensateHandler newInstance() {
		if (autoCompensateHandler == null) {
			synchronized(AutoCompensateHandler.class) {
				if (autoCompensateHandler == null) {
					autoCompensateHandler = new AutoCompensateHandler();
				}
			}
		}
		return autoCompensateHandler;
	}

	@Override
	public void saveAutoCompensationInfo(PreparedStatement delegate, String executeSql, boolean isBeforeNotice) throws SQLException {
		String localTxId = AutoCompensableCache.getLocalTxIdByCurThreadId();
		if (localTxId == null || localTxId.length() == 0) {
			return;
		}
		String server = "";// TODO
		
		// To parse SQL by SQLParser tools from Druid.
		MySqlStatementParser parser = new MySqlStatementParser(executeSql);
		SQLStatement sqlStatement = parser.parseStatement();
		
		if (isBeforeNotice && sqlStatement instanceof MySqlUpdateStatement) {
			AutoCompensateUpdateHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		} else if (!isBeforeNotice && sqlStatement instanceof MySqlInsertStatement) {
			AutoCompensateInsertHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
		} else if (isBeforeNotice && sqlStatement instanceof MySqlDeleteStatement) {
			AutoCompensateDeleteHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server);
//		} else if (isBeforeNotice && sqlStatement instanceof MySqlSelectIntoStatement) {// The select SQL would never enter into this area.
			// Nothing to do
		} else if (isBeforeNotice) {
			// TODO To define a switch which is named for 'CheckSpecialSQL', default is closed, means that just does record, if it's open, then program will throw an exception about current special SQL, just for auto-compensation.
			boolean checkSpecialSql = false;
			if (checkSpecialSql) {
				throw new SQLException(UtxConstants.logErrorPrefixWithTime() + "Do not support sql [" + executeSql + "] to auto-compensation.");
			} else {
				LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Do not support sql [{}] to auto-compensation, but it has been executed due to the switch is closed.", executeSql);
			}
		}
	}
	
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server) throws SQLException {
		return false;
	}

	protected String parsePrimaryKeyColumnName(PreparedStatement delegate, SQLStatement sqlStatement, String tableName) throws SQLException {
		String primaryKeyColumnName = "id";
		// So far, didn't know how to get primary-key from Druid. So, use the original way. 
		PreparedStatement ps = delegate.getConnection().prepareStatement("SHOW FULL COLUMNS FROM " + tableName + UtxConstants.ACTION_SQL);
		ResultSet columnResultSet = ps.executeQuery();
		while (columnResultSet.next()) {
			if ("PRI".equalsIgnoreCase(columnResultSet.getString("Key")) && primaryKeyColumnName.length() == 0) {
				primaryKeyColumnName = columnResultSet.getString("Field");
				break;
			}
		}
		return primaryKeyColumnName;
	}
	
	protected Map<String, String> selectColumnNameType(PreparedStatement delegate, String tableName) throws SQLException {
		PreparedStatement prepareStatement = delegate.getConnection().prepareStatement("SHOW FULL COLUMNS FROM " + tableName + UtxConstants.ACTION_SQL);
		ResultSet resultSet = prepareStatement.executeQuery();
		Map<String, String> columnNameType = new HashMap<>();
		while (resultSet.next()) {
			columnNameType.put(resultSet.getString(1), resultSet.getString(2));// column name and type
		}
		prepareStatement.close();
		return columnNameType;
	}

	protected void resetColumnValueByDBType(Map<String, String> columnNameType, Map<String, Object> dataMap) {
		for (String key : dataMap.keySet()) {
			// TODO support all of column type.
			String type = columnNameType.get(key);
			if (type == null && key.startsWith("n_c_v_")) {
				type = columnNameType.get(key.substring(6));
			}
			if (type.startsWith("varchar") || "datetime".equalsIgnoreCase(type)) {
				dataMap.put(key, "'" + dataMap.get(key) + "'");
			}
		}
	}

	protected String constructWhereSqlForCompensation(Map<String, Object> dataMap) throws SQLException {
		StringBuffer whereSqlForCompensation = new StringBuffer();
		for (String key : dataMap.keySet()) {
			if (!key.startsWith("n_c_v_")) {
				Object value = dataMap.get(key);
//				if (value == null) {
				if (dataMap.keySet().contains("n_c_v_" + key)) {
					value = dataMap.get("n_c_v_" + key);
				}
				if (whereSqlForCompensation.length() == 0) {
					whereSqlForCompensation.append(key + " = " + value);
				} else {
					whereSqlForCompensation.append(" and " + key + " = " + value);
				}
			}
		}
		return whereSqlForCompensation.toString();
	}
	
	/**
	 * To save auto-compensation info to data table 'saga_undo_log'.
	 * 
	 * @param preparedStatement
	 * @param connection
	 * @param localTxId
	 * @param executeSql
	 * @param compensateSql
	 * @param originalDataJson
	 * @param server
	 * @throws SQLException
	 * @author Gannalyo
	 * @since 2018-08-08
	 */
	public boolean saveSagaUndoLog(PreparedStatement delegate, String localTxId, String executeSql, String compensateSql, String originalDataJson, String server) throws SQLException {
		int index = 1;
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		PreparedStatement preparedStatement = null;
		try {
			String sql = "insert into saga_undo_log(globaltxid, localtxid, executesql, compensatesql, originalinfo, status, server, lastmodifytime, createtime) values (?, ?, ?, ?, ?, ?, ?, ?, ?)" + UtxConstants.ACTION_SQL;
			preparedStatement = delegate.getConnection().prepareStatement(sql);
			preparedStatement.setString(index++, AutoCompensableCache.getGlobalTxIdByCurThreadId());
			preparedStatement.setString(index++, localTxId);
			preparedStatement.setString(index++, executeSql);
			preparedStatement.setString(index++, compensateSql);
			preparedStatement.setString(index++, originalDataJson);
			preparedStatement.setInt(index++, 0);
			preparedStatement.setString(index++, server);
			preparedStatement.setTimestamp(index++, currentTime);
			preparedStatement.setTimestamp(index++, currentTime);
			return preparedStatement.executeUpdate() > 0;
		} finally {
			if (preparedStatement != null) {
				preparedStatement.close();
			}
		}
	}
	
}
