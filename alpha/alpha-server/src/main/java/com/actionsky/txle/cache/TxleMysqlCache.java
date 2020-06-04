/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.cache;

import com.actionsky.txle.enums.GlobalTxStatus;
import com.actionsky.txle.grpc.interfaces.ICustomRepository;
import org.apache.servicecomb.saga.alpha.core.TxleConsulClient;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.CrossSystemInetAddress;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TxleMysqlCache implements ITxleConsistencyCache {

    @Autowired
    private ICustomRepository customRepository;

    /**
     * 系统配置缓存，来自数据表KeyValueCache，但不包括每个全局事务的私有缓存
     * 一致性保证：系统配置更新后，从consul获取集群私有节点，通过resetAPI分别调用各节点的刷新系统配置缓存方法来刷新各节点的本地系统配置缓存，所有都ok则本次更新ok
     */
    private final Map<String, String> systemConfigCache = new ConcurrentHashMap<>();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TxleConsulClient consulClient;

    @Value("${server.port:8090}")
    private int serverPort;

    private int sevenDaysSeconds = 604800;
    private long thirtyCenturySeconds = 32503654861000L;

    @PostConstruct
    void init() {
        new Thread(() -> this.resetLocalSystemConfigCache()).start();
    }

    public Map<String, String> getSystemConfigCache() {
        return systemConfigCache;
    }

    private void reloadRemoteNodesSystemConfigCache() {
        Set<String> serversIPAndPort = consulClient.getServersIPAndPort();
        if (serversIPAndPort != null && !serversIPAndPort.isEmpty()) {
            try {
                String curIpPort = CrossSystemInetAddress.readCrossSystemIPv4() + ":" + serverPort;
                serversIPAndPort.remove(curIpPort);
                curIpPort = InetAddress.getLocalHost().getHostName() + ":" + serverPort;
                serversIPAndPort.remove(curIpPort);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            for (String ipPort : serversIPAndPort) {
                boolean synCacheResult = restTemplate.getForObject("http://" + ipPort + "/resetLocalSystemConfigCache", Boolean.class);
                if (!synCacheResult) {
                    throw new RuntimeException("Failed to execute the method 'reloadSystemConfigCache' for node [" + ipPort + "]");
                }
            }
        }
    }

    @Override
    public boolean resetLocalSystemConfigCache() {
        this.systemConfigCache.clear();
        List list = this.customRepository.executeQuery("SELECT * FROM KeyValueCache T");
        if (list != null && !list.isEmpty()) {
            list.forEach(obj -> {
                Object[] objArr = (Object[]) obj;
                String key = objArr[0] + "";
                if (!key.startsWith(TxleConstants.TXLE_TX_KEY)) {
                    this.systemConfigCache.put(key, objArr[1] + "");
                }
            });
        }
        return true;
    }

    @Override
    public boolean setKeyValueCache(String key, String value) {
        return this.setKeyValueCache(key, value, 0);
    }

    @Override
    public boolean setKeyValueCache(String key, String value, long expire) {
        Date expireDate;
        if (expire > 0) {
            expireDate = new Date(System.currentTimeMillis() + expire * 1000);
        } else {
            if (key.startsWith(TxleConstants.TXLE_TX_KEY)) {
                expireDate = new Date(System.currentTimeMillis() + sevenDaysSeconds * 1000);
            } else {
                expireDate = new Date(thirtyCenturySeconds);
            }
        }
        this.delete(key);
        boolean result = this.customRepository.executeUpdate("INSERT INTO KeyValueCache VALUES(?, ?, ?)", key, value, expireDate) > 0;
        if (result) {
            this.put(key, value);
        }
        return result;
    }

    @Override
    public int getKeyValueCacheCount() {
        List list = this.customRepository.executeQuery("SELECT count(*) FROM KeyValueCache");
        if (list != null && !list.isEmpty()) {
            Object value = list.get(0);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        }
        return 0;
    }

    @Override
    public String getValueByCacheKey(String key) {
        // 系统级缓存直接从内存缓存中读取即可
        if (isSystemConfigKey(key)) {
            return this.systemConfigCache.get(key);
        } else {
            // 事务及缓存从数据库缓存读取
            List list = this.customRepository.executeQuery("SELECT T.cachevalue FROM KeyValueCache T WHERE T.cachekey = ?", key);
            if (list != null && !list.isEmpty()) {
                Object value = list.get(0);
                if (value != null) {
                    return value.toString();
                }
            }
        }
        return null;
    }

    @Override
    public boolean getBooleanValue(String instanceId, String category, ConfigCenterType type) {
        /**
         * 逻辑：
         * 1.先检测type的全局配置是否支持功能，若不支持，直接返回false
         * 2.验证客户端对应的type配置，如果有配置则返回该配置值
         * 3.如果无客户端配置，则验证type的全局配置，如果有则返回
         * 4.如果没有则取type的默认值
         */
        if ((TxleConstants.NO + "").equals(this.getValueByCacheKey(TxleConstants.constructGlobalConfigAbilityKey(null, null, type)))) {
            return false;
        }

        if (instanceId != null) {
            String cacheValue = this.getValueByCacheKey(TxleConstants.constructGlobalConfigValueKey(instanceId, category, type));
            if (cacheValue != null) {
                return "true".equals(cacheValue) || TxleConstants.ENABLED.equals(cacheValue);
            }
        }

        String cacheValue = this.getValueByCacheKey(TxleConstants.constructGlobalConfigValueKey(null, null, type));
        if (cacheValue != null) {
            return "true".equals(cacheValue) || TxleConstants.ENABLED.equals(cacheValue);
        }

        return type.defaultValue();
    }

    @Override
    public Map<String, String> getValueListByCacheKey(String keyPrefix) {
        List list = this.customRepository.executeQuery("SELECT T.cachekey, T.cachevalue FROM KeyValueCache T WHERE T.cachekey LIKE CONCAT('', ?, '%')", keyPrefix);
        if (list != null && !list.isEmpty()) {
            Map<String, String> kvMap = new HashMap<>();
            list.forEach(obj -> {
                Object[] objArr = (Object[]) obj;
                kvMap.put(objArr[0] + "", objArr[1] + "");
            });
            return kvMap;
        }
        return null;
    }

    private Map<String, String> getValueListByCacheKeyValue(String keyPrefix, String value) {
        List list = this.customRepository.executeQuery("SELECT T.cachekey, T.cachevalue FROM KeyValueCache T WHERE T.cachekey LIKE CONCAT('', ?, '%') AND cachevalue = ?", keyPrefix, value);
        if (list != null && !list.isEmpty()) {
            Map<String, String> kvMap = new HashMap<>();
            list.forEach(obj -> {
                Object[] objArr = (Object[]) obj;
                kvMap.put(objArr[0] + "", objArr[1] + "");
            });
            return kvMap;
        }
        return null;
    }

    @Override
    public boolean deleteAll() {
        boolean result = this.customRepository.executeUpdate("DELETE FROM KeyValueCache") > 0;
        if (result) {
            this.systemConfigCache.clear();
            this.reloadRemoteNodesSystemConfigCache();
        }
        return result;
    }

    @Override
    public boolean delete(String key) {
        boolean result = this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey = ?", key) > 0;
        if (result) {
            this.remove(key);
        }
        return result;
    }

    @Override
    public boolean delete(String key, String value) {
        boolean result = this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey = ? AND cachevalue = ?", key, value) > 0;
        if (result) {
            this.remove(key);
        }
        return result;
    }

    @Override
    public boolean deleteByKeyPrefix(String keyPrefix) {
        Map<String, String> caches = this.getValueListByCacheKey(keyPrefix);
        if (caches != null && !caches.isEmpty()) {
            boolean result = this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey LIKE CONCAT('', ?, '%')", keyPrefix) > 0;
            if (result) {
                caches.keySet().forEach(key -> this.remove(key));
            }
            return result;
        }
        return true;
    }

    @Override
    public boolean deleteByKeyPrefix(String keyPrefix, String value) {
        Map<String, String> caches = this.getValueListByCacheKeyValue(keyPrefix, value);
        if (caches != null && !caches.isEmpty()) {
            boolean result = this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey LIKE CONCAT('', ?, '%') AND cachevalue = ?", keyPrefix, value) > 0;
            if (result && isSystemConfigKey(keyPrefix)) {
                caches.keySet().forEach(key -> this.remove(key));
            }
            return result;
        }
        return true;
    }

    private boolean isSystemConfigKey(String key) {
        return key.startsWith(TxleConstants.TXLE_CONFIG_KEY);
    }

    private String put(String key, String value) {
        if (isSystemConfigKey(key)) {
            this.reloadRemoteNodesSystemConfigCache();
            return this.systemConfigCache.put(key, value);
        }
        return null;
    }

    private String remove(String key) {
        if (isSystemConfigKey(key)) {
            this.reloadRemoteNodesSystemConfigCache();
            return this.systemConfigCache.remove(key);
        }
        return null;
    }

    public void clearExpiredAndOverTxCache() {
        this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE expire < now() AND cachekey LIKE CONCAT('', ?, '%') AND cachevalue <> ?", TxleConstants.TXLE_TX_KEY, GlobalTxStatus.Paused.toString());
    }

}
