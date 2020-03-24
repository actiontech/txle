/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository
public class CustomRepository implements ICustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List executeQuery(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);

        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }

        return query.getResultList();
    }

    public long count(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);

        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        List list = query.getResultList();
        return (long) list.get(0);
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
