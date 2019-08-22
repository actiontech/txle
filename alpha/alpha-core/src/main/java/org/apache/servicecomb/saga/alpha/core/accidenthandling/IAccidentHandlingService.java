package org.apache.servicecomb.saga.alpha.core.accidenthandling;

import java.util.List;
import java.util.Map;

/**
 * Accident Handling Repository.
 *
 * @author Gannalyo
 * @since 2019/06/14
 */
public interface IAccidentHandlingService {

    boolean save(AccidentHandling accidentHandling);

    List<AccidentHandling> findAccidentHandlingList();

    List<AccidentHandling> findAccidentHandlingList(AccidentHandleStatus status);

    boolean updateAccidentStatusByIdList(List<Long> idList, AccidentHandleStatus status);

    boolean reportMsgToAccidentPlatform(String jsonParams);

    List<Map<String, Object>> findAccidentList(int pageIndex, int pageSize, String orderName, String direction, String searchText);

    long findAccidentCount(String searchText);
}
