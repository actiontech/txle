/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.datadictionary;

import java.util.List;

public interface IDataDictionaryService {

    List<DataDictionaryItem> selectDataDictionaryList(String key);

    boolean createDataDictionary(DataDictionaryItem dataDictionaryItem);

    int selectMaxShowOrder(String key);

    List<String> selectGlobalTxServerNames();

    List<String> selectGlobalTxServerInstanceIds(String serverName);

    List<String> selectGlobalTxServerCategories(String serverName, String instanceId);

}
