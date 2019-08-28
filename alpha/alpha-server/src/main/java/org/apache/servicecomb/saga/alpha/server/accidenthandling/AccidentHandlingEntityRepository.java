/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.accidenthandling;

import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandling;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AccidentHandlingEntityRepository extends CrudRepository<AccidentHandling, Long> {

    @Query("FROM AccidentHandling T")
    List<AccidentHandling> findAccidentHandlingList(Pageable pageable);

    @Query("FROM AccidentHandling T WHERE T.status = ?1")
    List<AccidentHandling> findAccidentListByStatus(int status);

    @Transactional
    @Modifying
    @Query("UPDATE AccidentHandling T SET T.status = ?2 WHERE T.id IN ?1")
    int updateAccidentStatusByIdList(List<Long> idList, int status);

    @Query("FROM AccidentHandling T")
    List<AccidentHandling> findAccidentList(Pageable pageable);

    @Query("FROM AccidentHandling T WHERE FUNCTION('CONCAT_WS', ',', T.globaltxid, T.localtxid, T.servicename, T.bizinfo, FUNCTION('TXLE_DECODE', 'accident-handle-type', T.type), FUNCTION('TXLE_DECODE', 'accident-handle-status', T.status), T.createtime, T.completetime) LIKE CONCAT('%', ?1, '%')")
    List<AccidentHandling> findAccidentList(Pageable pageable, String searchText);

    @Query("SELECT COUNT(1) FROM AccidentHandling T")
    long findAccidentCount();

    @Query("SELECT COUNT(1) FROM AccidentHandling T WHERE FUNCTION('CONCAT_WS', ',', T.globaltxid, T.localtxid, T.servicename, T.bizinfo, FUNCTION('TXLE_DECODE', 'accident-handle-type', T.type)," +
            " FUNCTION('TXLE_DECODE', 'accident-handle-status', T.status), T.createtime, T.completetime) LIKE CONCAT('%', ?1, '%')")
    long findAccidentCount(String searchText);

}
