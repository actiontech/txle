package org.apache.servicecomb.saga.alpha.core.accidenthandling;

import java.util.List;

/**
 * Accident Handling Repository.
 *
 * @author Gannalyo
 * @date 2019/06/14
 */
public interface IAccidentHandlingRepository {

    boolean save(AccidentHandling accidentHandling);

    List<AccidentHandling> findAccidentHandlingList();

    List<AccidentHandling> findAccidentHandlingList(AccidentHandleStatus status);

    boolean updateAccidentStatusByIdList(List<Long> idList, AccidentHandleStatus status);

}
