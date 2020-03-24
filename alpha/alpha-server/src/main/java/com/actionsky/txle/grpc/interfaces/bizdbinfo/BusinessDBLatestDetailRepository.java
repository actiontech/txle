/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.bizdbinfo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface BusinessDBLatestDetailRepository extends CrudRepository<BusinessDBLatestDetail, Long> {

    @Query("SELECT COALESCE(MAX(T.timestamp), 0) FROM BusinessDBLatestDetail T")
    long selectMaxTimestamp();

    @Transactional
    @Modifying
    @Query("DELETE FROM BusinessDBLatestDetail T WHERE T.node = ?1 AND T.dbschema = ?2")
    int deleteHistoryInfo(String node, String dbSchema);

    @Transactional
    @Modifying
    @Query("DELETE FROM BusinessDBLatestDetail T WHERE T.timestamp < ?1 AND T.node = ?2 AND T.dbschema = ?3 AND T.tablename = ?4")
    int deleteHistoryInfo(long timestamp, String node, String dbSchema, String tableName);
}
