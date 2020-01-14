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

	int executeUpdate(String sql);

	List<Map<String, Object>> executeQuery(String sql, Object... params);

	int executeQueryCount(String sql, Object... params);

	void setDataSource(DataSource dataSource);

}
