/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.datadictionary;

import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;

import java.util.Date;
import java.util.List;

/**
 * @author Gannalyo
 * @since 2019/2/21
 */
public class DataDictionaryService implements IDataDictionaryService {

    private DataDictionaryEntityRepository dataDictionaryEntityRepository;

    public DataDictionaryService(DataDictionaryEntityRepository dataDictionaryEntityRepository) {
        this.dataDictionaryEntityRepository = dataDictionaryEntityRepository;
    }

    @Override
    public List<DataDictionaryItem> selectDataDictionaryList(String key) {
        return dataDictionaryEntityRepository.selectDataDictionaryListByKey(key);
    }

    @Override
    public boolean createDataDictionary(DataDictionaryItem dataDictionaryItem) {
        dataDictionaryItem.setCreatetime(new Date());
        return dataDictionaryEntityRepository.save(dataDictionaryItem) != null;
    }

    @Override
    public int selectMaxShowOrder(String key) {
        return dataDictionaryEntityRepository.selectMaxShowOrder(key);
    }

    @Override
    public List<String> selectGlobalTxServerNames() {
        return dataDictionaryEntityRepository.selectGlobalTxServerNames("global-tx-server-info");
    }

    @Override
    public List<String> selectGlobalTxServerInstanceIds(String serverName) {
        return dataDictionaryEntityRepository.selectGlobalTxServerInstances("global-tx-server-info", serverName);
    }

    @Override
    public List<String> selectGlobalTxServerCategories(String serverName, String instanceId) {
        return dataDictionaryEntityRepository.selectGlobalTxServerCategories("global-tx-server-info", serverName, instanceId);
    }
}
