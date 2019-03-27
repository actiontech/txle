package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlSelectIntoStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.transaction.DataSourceMappingCache;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

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
	public void saveAutoCompensationInfo(PreparedStatement delegate, String executeSql, boolean isBeforeNotice, Map<String, Object> standbyParams) throws SQLException {
		String localTxId = CurrentThreadOmegaContext.getLocalTxIdFromCurThread();
		if (localTxId == null || localTxId.length() == 0) {
			return;
		}

		// To parse SQL by SQLParser tools from Druid.
		MySqlStatementParser parser = new MySqlStatementParser(executeSql);
		SQLStatement sqlStatement = parser.parseStatement();
		if (sqlStatement instanceof MySqlSelectIntoStatement) {
			return;
		}

		if (standbyParams == null) {
			standbyParams = new HashMap<>();
		}

		String server = CurrentThreadOmegaContext.getServiceNameFromCurThread();

		// To set a relationship between localTxId and datSourceInfo, in order to determine to use the relative dataSource for localTxId when it need be compensated.
		DatabaseMetaData databaseMetaData = delegate.getConnection().getMetaData();
		String dburl = databaseMetaData.getURL(), dbusername = databaseMetaData.getUserName(), dbdrivername = databaseMetaData.getDriverName();
		DataSourceMappingCache.putLocalTxIdAndDataSourceInfo(localTxId, dburl, dbusername, dbdrivername);
		// To construct kafka message.
		standbyParams.put("dbdrivername", dbdrivername);
		standbyParams.put("dburl", dburl);
		standbyParams.put("dbusername", dbusername);

		if (isBeforeNotice && sqlStatement instanceof MySqlUpdateStatement) {
			AutoCompensateUpdateHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server, standbyParams);
		} else if (!isBeforeNotice && sqlStatement instanceof MySqlInsertStatement) {
			AutoCompensateInsertHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server, standbyParams);
		} else if (isBeforeNotice && sqlStatement instanceof MySqlDeleteStatement) {
			AutoCompensateDeleteHandler.newInstance().saveAutoCompensationInfo(delegate, sqlStatement, executeSql, localTxId, server, standbyParams);
		} else if (isBeforeNotice) {
			standbyParams.clear();
			// TODO To define a switch which is named for 'CheckSpecialSQL', default is closed, means that just does record, if it's open, then program will throw an exception about current special SQL, just for auto-compensation.
			boolean checkSpecialSql = false;
			if (checkSpecialSql) {
				throw new SQLException(UtxConstants.logErrorPrefixWithTime() + "Do not support sql [" + executeSql + "] to auto-compensation.");
			} else {
				LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Do not support sql [{}] to auto-compensation, but it has been executed due to the switch is closed.", executeSql);
			}
		}
	}

	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
		return false;
	}

	protected Map<String, String> selectColumnNameType(PreparedStatement delegate, String tableName) throws SQLException {
		String sql = "SHOW FULL COLUMNS FROM " + tableName + UtxConstants.ACTION_SQL;

		// start to mark duration for maintaining sql By Gannalyo.
		ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

		PreparedStatement prepareStatement = delegate.getConnection().prepareStatement(sql);
		ResultSet resultSet = prepareStatement.executeQuery();

		// end mark duration for maintaining sql By Gannalyo.
		ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

		Map<String, String> columnNameType = new HashMap<>();
		while (resultSet.next()) {
			columnNameType.put(resultSet.getString(1), resultSet.getString(2));// column name and type
		}
		prepareStatement.close();
		return columnNameType;
	}

	protected String parsePrimaryKeyColumnName(PreparedStatement delegate, SQLStatement sqlStatement, String tableName) throws SQLException {
		String primaryKeyColumnName = "id", sql = "SHOW FULL COLUMNS FROM " + tableName + UtxConstants.ACTION_SQL;

		// start to mark duration for maintaining sql By Gannalyo.
		ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

		// So far, didn't know how to get primary-key from Druid. So, use the original way.
		PreparedStatement ps = delegate.getConnection().prepareStatement(sql);
		ResultSet columnResultSet = ps.executeQuery();

		// end mark duration for maintaining sql By Gannalyo.
		ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

		while (columnResultSet.next()) {
			if ("PRI".equalsIgnoreCase(columnResultSet.getString("Key")) && primaryKeyColumnName.length() == 0) {
				primaryKeyColumnName = columnResultSet.getString("Field");
				break;
			}
		}
		return primaryKeyColumnName;
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
			preparedStatement.setString(index++, CurrentThreadOmegaContext.getGlobalTxIdFromCurThread());
			preparedStatement.setString(index++, localTxId);
			preparedStatement.setString(index++, executeSql);
			preparedStatement.setString(index++, compensateSql);
			preparedStatement.setString(index++, originalDataJson);
			preparedStatement.setInt(index++, 0);
			preparedStatement.setString(index++, server);
			preparedStatement.setTimestamp(index++, currentTime);
			preparedStatement.setTimestamp(index++, currentTime);

			// start to mark duration for maintaining sql By Gannalyo.
			ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(sql, false);

			boolean result = preparedStatement.executeUpdate() > 0;

			// end mark duration for maintaining sql By Gannalyo.
			ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

			return result;
		} catch (Exception e) {
			LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Failed to save undo_log, localTxId=[{}].", localTxId, e);
			return false;
		} finally {
			if (preparedStatement != null) {
				preparedStatement.close();
			}
		}
	}
	
}
