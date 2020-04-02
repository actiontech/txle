/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.List;

@Repository
public class CustomRepository {

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

    @Transactional
    public boolean callProcedure(String procedureName, Object... params) {
        StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(procedureName);
        storedProcedureQuery.registerStoredProcedureParameter("schema_name", String.class, ParameterMode.IN);
        storedProcedureQuery.registerStoredProcedureParameter("backup_tablename", String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter("schema_name", params[0]).setParameter("backup_tablename", params[1]);
        return storedProcedureQuery.execute();
    }

    @Transactional
    public int executeSubTxSqls(@Param("sqls") List<String> sqls) {
        int result = 0;
        for (String sql : sqls) {
            if (sql.startsWith("DROP PROCEDURE")) {
                // 模拟程序的写法，仅针对alter_txle_backup_table存储过程
                String backupTableName = sql.substring(sql.indexOf("txle.") + 5, sql.indexOf(" ADD globalTxId VARCHAR(36)"));
                this.callProcedure("alter_txle_backup_table", "txle", backupTableName);
                result++;
            } else {
                result += this.executeUpdate(sql);
            }
        }
        return result;
    }

}
