package org.apache.servicecomb.saga.alpha.server.datadictionary;

import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DataDictionaryEntityRepository extends CrudRepository<DataDictionaryItem, Long> {

    @Query("FROM DataDictionaryItem T WHERE T.ddcode = ?1 order by T.showorder")
    List<DataDictionaryItem> selectDataDictionaryListByKey(String key);

    @Query("SELECT coalesce(max(t.showorder), 0) FROM DataDictionaryItem t WHERE T.ddcode = ?1")
    int selectMaxShowOrder(String key);

    @Query("SELECT t.name FROM DataDictionaryItem t WHERE T.ddcode = ?1")
    List<String> selectGlobalTxServerNames(String key);

    @Query("SELECT t.code FROM DataDictionaryItem t WHERE T.ddcode = ?1 AND T.name = ?2")
    List<String> selectGlobalTxServerInstances(String key, String serverName);

    @Query("SELECT t.value FROM DataDictionaryItem t WHERE T.ddcode = ?1 AND T.name = ?2 AND T.code = ?3")
    List<String> selectGlobalTxServerCategories(String key, String serverName, String serverInstanceId);
}
