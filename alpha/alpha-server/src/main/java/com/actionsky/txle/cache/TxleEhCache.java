/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.cache;

import com.actionsky.txle.grpc.interfaces.ICustomRepository;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author Gannalyo
 * @since 2020/2/19
 */
public class TxleEhCache implements ITxleEhCache {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ICustomRepository customRepository;

    @Autowired
    private IConfigCenterService dbDegradationConfigService;

    @PostConstruct
    void init() {
        // initialize cache
        this.initializeConfigCache();
        this.initializeBusinessTablePKCache();
    }

    private void initializeConfigCache() {
        List<ConfigCenter> configCenterList = dbDegradationConfigService.selectConfigCenterList();
        if (configCenterList != null && !configCenterList.isEmpty()) {
            Cache initCache = cacheManager.getCache(CacheName.CONFIG.toString());
            configCenterList.forEach(cfg -> {
                String configKey = TxleConstants.constructConfigCacheKey(cfg.getInstanceid(), cfg.getCategory(), cfg.getType());
                if (cfg.getStatus() == ConfigCenterStatus.Normal.toInteger()) {
                    initCache.put(new Element(configKey, cfg.getAbility() == TxleConstants.YES ? cfg.getValue() : TxleConstants.DISABLED));
                }
            });
        }
    }

    private void initializeBusinessTablePKCache() {
        final Cache initCache = cacheManager.getCache(CacheName.INIT.toString());
        if (initCache.getKeys().isEmpty()) {
            List list = customRepository.executeQuery("SELECT T.node, T.dbschema, T.tablename, T.field FROM BusinessDBLatestDetail T WHERE T.isprimarykey = 1");
            if (list != null && !list.isEmpty()) {
                list.forEach(obj -> {
                    Object[] objArr = (Object[]) obj;
                    initCache.put(new Element(objArr[0] + "." + objArr[1] + "." + objArr[2], objArr[3]));
                });
            }
        }
    }

    public boolean readConfigCache(String serviceInstanceId, String category, ConfigCenterType type) {
        Element element = readConfigCacheElement(serviceInstanceId, category, type);
        if (element != null) {
            Object value = element.getObjectValue();
            if (TxleConstants.ENABLED.equals(value) || "true".equals(value)) {
                return true;
            } else if (TxleConstants.DISABLED.equals(value) || "false".equals(value)) {
                return false;
            }
        }
        return type.defaultValue();
    }

    public int readIntConfigCache(String serviceInstanceId, String category, ConfigCenterType type) {
        Element element = readConfigCacheElement(serviceInstanceId, category, type);
        if (element != null) {
            return (int) element.getObjectValue();
        }
        return type.defaultIntValue();
    }

    private Element readConfigCacheElement(String serviceInstanceId, String category, ConfigCenterType type) {
        String configKey = TxleConstants.constructConfigCacheKey(serviceInstanceId, category, type.toInteger());
        Element element = cacheManager.getCache(CacheName.CONFIG.toString()).get(configKey);
        if (element == null) {
            configKey = TxleConstants.constructConfigCacheKey(null, null, type.toInteger());
            element = cacheManager.getCache(CacheName.CONFIG.toString()).get(configKey);
        }
        return element;
    }

    public CurrentGlobalTxCache readGlobalTxCache(String globalTxId) {
        /**
         * 读取全局事务缓存
         * 1.如果有缓存，则直接返回
         * 2.如果没有缓存，理论讲不能代表未注册，需通过读取数据库方可确定是否注册过，但此处仍然不再读取数据库，
         *      原因是读出的缓存仅用于判断是否注册，但不判断直接注册，注册过就失败，没注册过直接注册成功，可节省一步查询
         */
        Cache globalTxCache = cacheManager.getCache(CacheName.GLOBALTX.toString());
        Element element = globalTxCache.get(globalTxId);
        if (element != null) {
            return (CurrentGlobalTxCache) element.getObjectValue();
        }
        return null;
    }

    public void putGlobalTxCache(CurrentGlobalTxCache txCacheEntity) {
        if (txCacheEntity != null) {
            Cache cache = cacheManager.getCache(CacheName.GLOBALTX.toString());
            cache.remove(txCacheEntity.getGlobalTxId());
            cache.put(new Element(txCacheEntity.getGlobalTxId(), txCacheEntity));
        }
    }

    public void put(CacheName cacheName, String key, Object value) {
        cacheManager.getCache(cacheName.toString()).put(new Element(key, value));
    }

    public void put(CacheName cacheName, String key, Object value, int timeout) {
        Element element = new Element(key, value);
        element.setTimeToLive(timeout);
        cacheManager.getCache(cacheName.toString()).put(element);
    }

    public void putIfAbsent(CacheName cacheName, String key, Object value) {
        cacheManager.getCache(cacheName.toString()).putIfAbsent(new Element(key, value));
    }

    public void putIfAbsent(CacheName cacheName, String key, Object value, int timeout) {
        Element element = new Element(key, value);
        element.setTimeToLive(timeout);
        cacheManager.getCache(cacheName.toString()).putIfAbsent(element);
    }

    public Object get(CacheName cacheName, String key) {
        Element element = cacheManager.getCache(cacheName.toString()).get(key);
        if (element != null) {
            return element.getObjectValue();
        }
        return null;
    }

    public List<String> getKeys(CacheName cacheName) {
        return cacheManager.getCache(cacheName.toString()).getKeys();
    }

    public void remove(CacheName cacheName, String key) {
        cacheManager.getCache(cacheName.toString()).remove(key);
    }

}
