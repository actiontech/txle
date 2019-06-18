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

}
