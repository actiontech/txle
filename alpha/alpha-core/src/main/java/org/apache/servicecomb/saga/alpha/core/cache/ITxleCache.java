/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Gannalyo
 * @since 2019/8/29
 */
public interface ITxleCache {
    ConcurrentHashMap<String, Boolean> getConfigCache();

    ConcurrentSkipListSet<CacheEntity> getTxSuspendStatusCache();

    ConcurrentSkipListSet<CacheEntity> getTxAbortStatusCache();

    boolean getTxSuspendStatus(String globalTxId);

    boolean getTxAbortStatus(String globalTxId);

    void putDistributedConfigCache(String key, Boolean value);

    void putDistributedTxSuspendStatusCache(String key, Boolean value, int expire);

    void putDistributedTxAbortStatusCache(String key, Boolean value, int expire);

    void removeDistributedConfigCache(String key);

    void removeDistributedTxStatusCache(Set<String> globalTxIdSet);

    void removeDistributedTxSuspendStatusCache(String key);

    void removeDistributedTxAbortStatusCache(String key);

    void putLocalConfigCache(String key, Boolean value);

    void putLocalTxSuspendStatusCache(String key, Boolean value, int expire);

    void putLocalTxAbortStatusCache(String key, Boolean value, int expire);

    void removeLocalConfigCache(String key);

    void removeLocalTxStatusCache(String key);

    void removeLocalTxSuspendStatusCache(String key);

    void removeLocalTxAbortStatusCache(String key);

    void refreshServiceListCache(boolean refreshRemoteServiceList);

    void synchronizeCacheFromLeader(String consulSessionId);

}
