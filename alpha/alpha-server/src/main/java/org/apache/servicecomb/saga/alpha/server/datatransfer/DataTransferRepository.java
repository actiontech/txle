package org.apache.servicecomb.saga.alpha.server.datatransfer;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

@Repository
public class DataTransferRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);
//        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.TO_LIST);

        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }

        return query.getResultList();
    }

    @Transactional
    public int executeUpdate(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);

        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }

        return query.executeUpdate();
    }

}
