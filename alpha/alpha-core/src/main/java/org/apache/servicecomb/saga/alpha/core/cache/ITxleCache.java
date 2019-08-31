/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Gannalyo
 * @since 2019/8/29
 */
public interface ITxleCache {
    ConcurrentHashMap<String, Boolean> getConfigCache();

    ConcurrentHashMap<String, Boolean> getTxSuspendStatusCache();

    void putForDistributedConfigCache(String key, Boolean value);

    void putForDistributedTxSuspendStatusCache(String key, Boolean value);

    void removeForDistributedConfigCache(String key);

    void removeForDistributedTxStatusCache(String key);

}
