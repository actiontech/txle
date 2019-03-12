package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.fastjson.JSON;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySqlDeleteHandler extends AutoCompensateDeleteHandler {

	private static MySqlDeleteHandler mySqlDeleteHandler = null;
	private static final Logger LOG = LoggerFactory.getLogger(MySqlDeleteHandler.class);

	public static MySqlDeleteHandler newInstance() {
		if (mySqlDeleteHandler == null) {
			synchronized (MySqlDeleteHandler.class) {
				if (mySqlDeleteHandler == null) {
					mySqlDeleteHandler = new MySqlDeleteHandler();
				}
			}
		}
		return mySqlDeleteHandler;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
		ResultSet rs = null;
		try {
			MySqlDeleteStatement deleteStatement = (MySqlDeleteStatement) sqlStatement;
			// 1.take table's name out
			SQLName table = deleteStatement.getTableName();
			String tableName = table.toString();
			standbyParams.put("tablename",tableName);
			standbyParams.put("operation", "delete");

			// 2.take conditions out
			SQLExpr where = deleteStatement.getWhere();// select * ... by where ... ps: having, etc.
			String whereSql = where.toString();// It doesn't matter, even though the 'where-sql' contains a line break.
			LOG.debug(UtxConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - table: [{}] - where: [{}].", Thread.currentThread().getId(), tableName, whereSql);

			// 3.take primary-key name
			String primaryKeyColumnName = this.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);
			
			// 4.take the original data out and put the lock on data.
			List<Map<String, Object>> originalData = selectOriginalData(delegate, deleteStatement, tableName, primaryKeyColumnName, whereSql);
			if (originalData == null || originalData.isEmpty()) {
				LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Did not save compensation info to table 'Saga_Undo_Log' due to the executeSql's result hadn't any effect to data. localTxId: [{}], server: [{}].", localTxId, server);
				return true;
			}
			StringBuffer ids = new StringBuffer();
			originalData.forEach(map -> {
				if (ids.length() == 0) {
					ids.append(map.get(primaryKeyColumnName));
				} else {
					ids.append(", " + map.get(primaryKeyColumnName));
				}
			});
			standbyParams.put("ids", ids);

			String originalDataJson = JSON.toJSONString(originalData);

			// PS: Do not joint the conditions of 'executeSql' to 'compensateSql' immediately, because the result may not be same to execute a SQL twice at different time.
			// Recommendation: construct compensateSql by original data.
			// 5.construct compensateSql
			String compensateSql = constructCompensateSql(delegate, deleteStatement, tableName, primaryKeyColumnName, originalData);
			
			// 6.save saga_undo_log
			return this.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, originalDataJson, server);
		} catch (SQLException e) {
			LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for delete sql.", e);
			throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for delete SQL.", e);
				}
			}
		}
	}

	private String constructCompensateSql(PreparedStatement delegate, MySqlDeleteStatement deleteStatement, String tableName, String primaryKeyColumnName, List<Map<String, Object>> originalData) throws SQLException {
		Map<String, String> columnNameType = this.selectColumnNameType(delegate, tableName);
		StringBuffer compensateSqls = new StringBuffer();
		originalData.forEach(dataMap -> {
			this.resetColumnValueByDBType(columnNameType, dataMap);
			StringBuffer columns = new StringBuffer();
			StringBuffer placeholder = new StringBuffer();
			dataMap.keySet().forEach(key -> {
				if (columns.length() == 0) {
					columns.append(key);
					placeholder.append("%s");
				} else {
					columns.append(", " + key);
					placeholder.append(", %s");
				}
			});
			
			String compensateSql = String.format("INSERT IGNORE INTO " + tableName + " (" + columns + ") VALUES (" + placeholder + ") " + UtxConstants.ACTION_SQL + ";", dataMap.values().toArray());
			if (compensateSqls.length() == 0) {
				compensateSqls.append(compensateSql);
			} else {
				compensateSqls.append("\n" + compensateSql);
			}
		});
		
		return compensateSqls.toString();
	}

	private List<Map<String, Object>> selectOriginalData(PreparedStatement delegate, MySqlDeleteStatement deleteStatement, String tableName, String primaryKeyColumnName, String whereSql) throws SQLException {
		PreparedStatement preparedStatement = null;
		try {
			String originalDataSql = String.format("SELECT * FROM %s WHERE %s FOR UPDATE " + UtxConstants.ACTION_SQL, tableName, whereSql);// 'FOR UPDATE' is needed to lock data.
			LOG.debug(UtxConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - originalDataSql: [{}].", Thread.currentThread().getId(), originalDataSql);

			// start to mark duration for business sql By Gannalyo.
			ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(originalDataSql, false);

			preparedStatement = delegate.getConnection().prepareStatement(originalDataSql);
			List<Map<String, Object>> originalDataList = new ArrayList<Map<String, Object>>();
			ResultSet dataResultSet = preparedStatement.executeQuery();

			// end mark duration for maintaining sql By Gannalyo.
			ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

			while (dataResultSet.next()) {
				Map<String, Object> dataMap = new HashMap<String, Object>();
				ResultSetMetaData metaData = dataResultSet.getMetaData();
				for (int i = 1; i <= metaData.getColumnCount(); i ++) {
					String column = metaData.getColumnName(i);
					dataMap.put(column, dataResultSet.getObject(column));
				}
				
				originalDataList.add(dataMap);
			}
			return originalDataList;
		} finally {
			if (preparedStatement != null) {
				preparedStatement.close();
			}
		}
	}

}
