/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * The data interface for auto-compensation.
 * Aim to execute complex and special SQL.
 *
 * @author Gannalyo
 * @since 201807-30
 */
public interface IAutoCompensateDao {

	/**
	 * To execute the dynamic auto-compensation SQL.
	 *
	 * @param autoCompensateSql SQL for auto compensation.
	 * @return result
	 * @author Gannalyo
	 * @since 201807-30
	 */
	boolean executeAutoCompensateSql(String autoCompensateSql);

	List<Map<String, Object>> execute(String sql, Object... params);

	void setDataSource(DataSource dataSource);

}
