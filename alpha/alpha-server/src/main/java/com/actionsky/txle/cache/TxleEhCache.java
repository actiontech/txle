/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.cache;

import com.actionsky.txle.grpc.interfaces.ICustomRepository;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

public class TxleEhCache implements ITxleEhCache {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ICustomRepository customRepository;

    @PostConstruct
    void init() {
        this.initializeBusinessTablePKCache();
        this.initializeExecutedBackupTable();
    }

    private void initializeBusinessTablePKCache() {
        List list = customRepository.executeQuery("SELECT T.node, T.dbschema, T.tablename, T.field FROM BusinessDBLatestDetail T WHERE T.isprimarykey = 1");
        if (list != null && !list.isEmpty()) {
            list.forEach(obj -> {
                Object[] objArr = (Object[]) obj;
                this.put(TxleCacheType.INIT, objArr[0] + "." + objArr[1] + "." + objArr[2], objArr[3]);
            });
        }
    }

    private void initializeExecutedBackupTable() {
        List list = customRepository.executeQuery("SELECT T.dbnodeid, T.dbschema, T.backuptablename FROM BusinessDBBackupInfo T");
        if (list != null && !list.isEmpty()) {
            list.forEach(obj -> {
                Object[] objArr = (Object[]) obj;
                this.put(TxleCacheType.OTHER, objArr[0] + "_" + objArr[1] + "_" + objArr[2], true);
            });
        }
    }

    public boolean readConfigCache(String serviceInstanceId, String category, ConfigCenterType type) {
        return readConfigCache(serviceInstanceId, category, type, null);
    }

    public boolean readConfigCache(String serviceInstanceId, String category, ConfigCenterType type, String globalTxId) {
        Element element = readConfigCacheElement(serviceInstanceId, category, type, globalTxId);
        if (element != null) {
            Object value = element.getObjectValue();
            if (value != null) {
                if (TxleConstants.ENABLED.equals(value.toString()) || "true".equals(value.toString())) {
                    return true;
                } else if (TxleConstants.DISABLED.equals(value.toString()) || "false".equals(value.toString())) {
                    return false;
                }
            }
        }
        return type.defaultValue();
    }

    public int readIntConfigCache(String serviceInstanceId, String category, ConfigCenterType type) {
        Element element = readConfigCacheElement(serviceInstanceId, category, type, null);
        if (element != null) {
            return (int) element.getObjectValue();
        }
        return type.defaultIntValue();
    }

    private Element readConfigCacheElement(String serviceInstanceId, String category, ConfigCenterType type, String globalTxId) {
        String configKey = TxleConstants.constructGlobalConfigValueKey(serviceInstanceId, category, type);
        if (globalTxId != null) {
            configKey = TxleConstants.constructTxConfigCacheKey(globalTxId);
        }
        Element element = cacheManager.getCache(TxleCacheType.CONFIG.toString()).get(configKey);
        if (element == null) {
            configKey = TxleConstants.constructGlobalConfigValueKey(null, null, type);
            element = cacheManager.getCache(TxleCacheType.CONFIG.toString()).get(configKey);
        }
        return element;
    }

    public void put(TxleCacheType txleCacheType, String key, Object value) {
        cacheManager.getCache(txleCacheType.toString()).put(new Element(key, value));
    }

    public void put(TxleCacheType txleCacheType, String key, Object value, int timeout) {
        Element element = new Element(key, value);
        element.setTimeToLive(timeout);
        cacheManager.getCache(txleCacheType.toString()).put(element);
    }

    public void putIfAbsent(TxleCacheType txleCacheType, String key, Object value) {
        cacheManager.getCache(txleCacheType.toString()).putIfAbsent(new Element(key, value));
    }

    public void putIfAbsent(TxleCacheType txleCacheType, String key, Object value, int timeout) {
        Element element = new Element(key, value);
        element.setTimeToLive(timeout);
        cacheManager.getCache(txleCacheType.toString()).putIfAbsent(element);
    }

    public Object get(TxleCacheType txleCacheType, String key) {
        Element element = cacheManager.getCache(txleCacheType.toString()).get(key);
        if (element != null) {
            return element.getObjectValue();
        }
        return null;
    }

    public Boolean getBooleanValue(TxleCacheType txleCacheType, String key) {
        Element element = cacheManager.getCache(txleCacheType.toString()).get(key);
        if (element != null) {
            return "true".equals(element.getObjectValue().toString()) || TxleConstants.ENABLED.equals(element.getObjectValue().toString());
        }
        return null;
    }

    public List<String> getKeys(TxleCacheType txleCacheType) {
        return cacheManager.getCache(txleCacheType.toString()).getKeys();
    }

    public void remove(TxleCacheType txleCacheType, String key) {
        cacheManager.getCache(txleCacheType.toString()).remove(key);
    }

    @Override
    public void removeGlobalTxCache(String globalTxId) {
        this.remove(TxleCacheType.GLOBALTX, globalTxId);
        this.remove(TxleCacheType.OTHER, "is-executed-backup-table-" + globalTxId);
    }
}
