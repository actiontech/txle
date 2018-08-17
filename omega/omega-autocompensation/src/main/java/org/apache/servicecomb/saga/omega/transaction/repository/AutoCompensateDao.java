package org.apache.servicecomb.saga.omega.transaction.repository;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

/**
 * The data interface for auto-compensation. Aim to execute complex and special SQL.
 * 
 * @author Gannalyo
 * @since 201807-30
 */
@Repository
public class AutoCompensateDao extends BaseDao implements IAutoCompensateDao {

	@PersistenceContext
	private EntityManager entityManager;

	@Modifying
	@Transactional
	@Override
	public boolean executeAutoCompensateSql(String autoCompensateSql) {
		Query query = entityManager.createNativeQuery(autoCompensateSql);
		return query.executeUpdate() > 0;
	}

	@Override
	public List<Map<String, Object>> execute(String sql, Object... params) {
		return super.execute(sql, params);
	}

}
