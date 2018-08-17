package org.apache.servicecomb.saga.omega.transaction.repository;

import java.util.List;
import java.util.Map;

/**
 * The root for data interface.
 * 
 * @author Gannalyo
 * @since 2018-08-06
 */
public interface IBaseDao {

	/**
	 * Executing dynamic SQL and parameters.
	 * 
	 * @param sql anyone SQL
	 * @param params any parameters
	 * @return result
	 * @author Gannalyo
	 * @since 2018-08-06
	 */
	public List<Map<String, Object>> execute(String sql, Object... params);

}
