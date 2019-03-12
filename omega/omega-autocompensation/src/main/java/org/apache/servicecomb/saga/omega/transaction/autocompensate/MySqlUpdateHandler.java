package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
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

public class MySqlUpdateHandler extends AutoCompensateUpdateHandler {

	private static MySqlUpdateHandler mySqlUpdateHandler = null;
	private static final Logger LOG = LoggerFactory.getLogger(MySqlUpdateHandler.class);

	public static MySqlUpdateHandler newInstance() {
		if (mySqlUpdateHandler == null) {
			synchronized (MySqlUpdateHandler.class) {
				if (mySqlUpdateHandler == null) {
					mySqlUpdateHandler = new MySqlUpdateHandler();
				}
			}
		}
		return mySqlUpdateHandler;
	}
	
	@Override
	public boolean saveAutoCompensationInfo(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {
		ResultSet rs = null;
		try {
			MySqlUpdateStatement updateStatement = (MySqlUpdateStatement) sqlStatement;
			// 1.take table's name out
			SQLName table = updateStatement.getTableName();
			String tableName = table.toString();
			standbyParams.put("tablename",tableName);
			standbyParams.put("operation", "update");

			// 2.take conditions out
			SQLExpr where = updateStatement.getWhere();// select * ... by where ... ps: having, etc.
			String whereSql = where.toString();// It doesn't matter, even though the 'where-sql' contains a line break.
			LOG.debug(UtxConstants.logDebugPrefixWithTime() + "currentThreadId: [{}] - table: [{}] - where: [{}].", Thread.currentThread().getId(), tableName, whereSql);

			// 3.take primary-key name
			String primaryKeyColumnName = this.parsePrimaryKeyColumnName(delegate, sqlStatement, tableName);
			
			// 4.take the original data out and put a lock on data.
			/**
			 * 此处逻辑修改：
			 * 1.补偿SQL追加所有字段最新值(含本次最新修改的)作为where条件，目的是检测后续执行补偿时，数据是否被修改。如被修改过则肯定执行失败，则报差错平台，如未被修改则正常执行补偿SQL(若执行失败也是要上报差错平台)。
			 * 2.获取本次修改后的值，因本次修改值中可能包含公式(如money = money - 10)，所以需通过修改条件先将修改后的值查询出，如：select money - 10 n_c_v_money from tableName where ... 。
			 * 3.考虑到查询原数据与上面SQL除查询字段其它都一致情况，故为了减少对数据库的访问，将元数据与本次更新后的值一同查出，查出后采用java代码进行处理，要比访问数据库性能层面节省很多
			 * 4.由于上面一次性将新老数据都查出，故后续代码分别使用新老数据之处要做一些新老数据的分离
			 */
			List<Map<String, Object>> newDataList = selectNewDataList(delegate, updateStatement, tableName, primaryKeyColumnName, whereSql);
			List<Map<String, Object>> originalDataList = selectOriginalData(newDataList);
			if (originalDataList == null || originalDataList.isEmpty()) {
				LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Did not save compensation info to table 'Saga_Undo_Log' due to the executeSql's result hadn't any effect to data. localTxId: [{}], server: [{}].", localTxId, server);
				return true;
			}
			StringBuffer ids = new StringBuffer();
			originalDataList.forEach(map -> {
				if (ids.length() == 0) {
					ids.append(map.get(primaryKeyColumnName));
				} else {
					ids.append(", " + map.get(primaryKeyColumnName));
				}
			});
			standbyParams.put("ids", ids);

			String originalDataJson = JSON.toJSONString(originalDataList);

			// PS: Do not joint the conditions of 'executeSql' to 'compensateSql' immediately, because the result may not be same to execute a SQL twice at different time.
			// Recommendation: construct compensateSql by original data.
			// 5.construct compensateSql
			String compensateSql = constructCompensateSql(delegate, updateStatement, tableName, newDataList, whereSql);
			
			// 6.save saga_undo_log
			return this.saveSagaUndoLog(delegate, localTxId, executeSql, compensateSql, originalDataJson, server);
		} catch (SQLException e) {
			LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to save auto-compensation info for update SQL.", e);
			throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to close ResultSet after executing method 'saveAutoCompensationInfo' for update SQL.", e);
				}
			}
		}
	}

	private String constructCompensateSql(PreparedStatement delegate, MySqlUpdateStatement updateStatement, String tableName, List<Map<String, Object>> newDataList, String whereSql) throws SQLException {
		if (newDataList == null || newDataList.isEmpty()) {
			throw new SQLException(UtxConstants.LOG_ERROR_PREFIX + "Could not get the original data when constructed the 'compensateSql' for executing update SQL.");
		}

		List<SQLUpdateSetItem> updateSetItemList = updateStatement.getItems();
		if (updateSetItemList == null || updateSetItemList.isEmpty()) {
			throw new SQLException(UtxConstants.LOG_ERROR_PREFIX + "Had no set-item for update SQL.");
		}
		
		Map<String, String> columnNameType = this.selectColumnNameType(delegate, tableName);
		StringBuffer compensateSqls = new StringBuffer();
		for (Map<String, Object> dataMap : newDataList) {
			this.resetColumnValueByDBType(columnNameType, dataMap);
			String setColumns = constructSetColumns(updateSetItemList, dataMap);
			String whereSqlForCompensation = this.constructWhereSqlForCompensation(dataMap);
			
			String compensateSql = String.format("UPDATE %s SET %s WHERE %s" + UtxConstants.ACTION_SQL + ";", tableName, setColumns, whereSqlForCompensation);
			if (compensateSqls.length() == 0) {
				compensateSqls.append(compensateSql);
			} else {
				compensateSqls.append("\n" + compensateSql);
			}
		}
		
		return compensateSqls.toString();
	}

	private String constructSetColumns(List<SQLUpdateSetItem> updateSetItemList, Map<String, Object> dataMap) {
		StringBuffer setColumns = new StringBuffer();
		for (SQLUpdateSetItem setItem : updateSetItemList) {
			String column = setItem.getColumn().toString();
			if (setColumns.length() == 0) {
				setColumns.append(column + " = " + dataMap.get(column));
			} else {
				setColumns.append(", " + column + " = " + dataMap.get(column));
			}
		}
		return setColumns.toString();
	}

	private List<Map<String, Object>> selectNewDataList(PreparedStatement delegate, MySqlUpdateStatement updateStatement, String tableName, String primaryKeyColumnName, String whereSql) throws SQLException {
		PreparedStatement preparedStatement = null;
		try {
			List<SQLUpdateSetItem> updateSetItemList = updateStatement.getItems();
			if (updateSetItemList == null || updateSetItemList.isEmpty()) {
				throw new SQLException("Have no set-item for update SQL.");
			}
			
			StringBuffer newColumnValues = new StringBuffer();
			for (SQLUpdateSetItem setItem : updateSetItemList) {
				String columnValue = setItem.getValue().toString();
				if (newColumnValues.length() > 0) {
					newColumnValues.append(", ");
				}
				newColumnValues.append(columnValue + " n_c_v_" + setItem.getColumn().toString());
			}
			
			String originalDataSql = String.format("SELECT T.*, %s FROM %s T WHERE %s FOR UPDATE" + UtxConstants.ACTION_SQL, newColumnValues, tableName, whereSql);// 'FOR UPDATE' is needed to lock data.
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
	
	private List<Map<String, Object>> selectOriginalData(List<Map<String, Object>> newDataList) {
		List<Map<String, Object>> originalDataList = new ArrayList<>();
		for (Map<String,Object> newDataMap : newDataList) {
			Map<String,Object> originalDataMap = new HashMap<>();
			for (String key : newDataMap.keySet()) {
				if (!key.startsWith("n_c_v_")) {
					originalDataMap.put(key, newDataMap.get(key));
				}
			}
			originalDataList.add(originalDataMap);
		}
		return originalDataList;
	}

}
