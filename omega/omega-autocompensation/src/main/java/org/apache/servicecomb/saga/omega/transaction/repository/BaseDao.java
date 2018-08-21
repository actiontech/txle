package org.apache.servicecomb.saga.omega.transaction.repository;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class BaseDao implements IBaseDao {

	@PersistenceContext
	public EntityManager entityManager;

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> execute(String sql, Object... params) {
		Query query = entityManager.createNativeQuery(sql);
	    query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
	    
	    for (int i = 0; i < params.length; i ++) {
	    	query.setParameter(i + 1, params[i]);
	    }
	    
		return query.getResultList();
	}

}
