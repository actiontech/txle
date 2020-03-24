/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.repository.primary;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.List;

@Repository
public class PrimaryCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List executeQuery(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);

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

    @Transactional
    public boolean callProcedure(String procedureName, Object... params) {
        StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(procedureName);
        storedProcedureQuery.registerStoredProcedureParameter("schema_name", String.class, ParameterMode.IN);
        storedProcedureQuery.registerStoredProcedureParameter("backup_tablename", String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter("schema_name", params[0]).setParameter("backup_tablename", params[1]);
        return storedProcedureQuery.execute();
    }

}
