/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.cache;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Service;
import com.ecwid.consul.v1.session.model.Session;
import org.apache.servicecomb.saga.alpha.core.cache.CacheEntity;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY;
import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY_VALUE;

/**
 * @author Gannalyo
 * @since 2019/8/29
 */
public class TxleCache implements ITxleCache {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ConcurrentHashMap<String, Boolean> configCache = new ConcurrentHashMap<>();
    // Store the identifies of global transaction when they have been suspended only. Do not use the 'configCache' variable so that free up memory for this variable in an even better fashion.
    private final ConcurrentSkipListSet<CacheEntity> txSuspendStatusCache = new ConcurrentSkipListSet<>(Comparator.comparingLong(s -> s.getExpire()));
    private final ConcurrentSkipListSet<CacheEntity> txAbortStatusCache = new ConcurrentSkipListSet<>(Comparator.comparingLong(s -> s.getExpire()));
    private final Set<String> serviceList = new HashSet();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private ConsulClient consulClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port:8090}")
    private int serverPort;

    public ConcurrentHashMap<String, Boolean> getConfigCache() {
        return configCache;
    }

    public ConcurrentSkipListSet<CacheEntity> getTxSuspendStatusCache() {
        return txSuspendStatusCache;
    }

    public ConcurrentSkipListSet<CacheEntity> getTxAbortStatusCache() {
        return txAbortStatusCache;
    }

    @Override
    public boolean getTxSuspendStatus(String globalTxId) {
        return getTxStatusByGlobalTxId(txSuspendStatusCache, globalTxId);
    }

    @Override
    public boolean getTxAbortStatus(String globalTxId) {
        return getTxStatusByGlobalTxId(txAbortStatusCache, globalTxId);
    }

    private boolean getTxStatusByGlobalTxId(ConcurrentSkipListSet<CacheEntity> txStatusCache, String globalTxId) {
        if (globalTxId != null) {
            Iterator<CacheEntity> cacheEntities = txStatusCache.iterator();
            while (cacheEntities.hasNext()) {
                CacheEntity cacheEntity = cacheEntities.next();
                if (cacheEntity != null && globalTxId.equals(cacheEntity.getKey())) {
                    // return true event though expired, due to it's not a real cache period, it's used for clearing cache mainly.
                    // in fact, the expires is not used yet so far.
                    return true;
                }
            }
        }
        return false;
    }

    public void putDistributedConfigCache(String key, Boolean value) {
        refreshDistributedCache(key, value.toString(), 0, "/putConfigCache");
    }

    public void putDistributedTxSuspendStatusCache(String key, Boolean value, int expire) {
        refreshDistributedCache(key, value.toString(), expire, "/putTxSuspendStatusCache");
    }

    public void putDistributedTxAbortStatusCache(String key, Boolean value, int expire) {
        refreshDistributedCache(key, value.toString(), expire, "/putTxAbortStatusCache");
    }

    public void removeDistributedConfigCache(String key) {
        refreshDistributedCache(key, "", 0, "/removeConfigCache");
    }

    public void removeDistributedTxStatusCache(Set<String> globalTxIdSet) {
        if (globalTxIdSet != null && !globalTxIdSet.isEmpty()) {
            StringBuilder keys = new StringBuilder(globalTxIdSet.size() * (36 + TxleConstants.STRING_SEPARATOR.length()));
            globalTxIdSet.forEach(key -> keys.append(key + TxleConstants.STRING_SEPARATOR));
            refreshDistributedCache(keys.toString(), "", 0, "/removeTxStatusCache");
        }
    }

    public void removeDistributedTxSuspendStatusCache(String key) {
        refreshDistributedCache(key, "", 0, "/removeTxSuspendStatusCache");
    }

    public void removeDistributedTxAbortStatusCache(String key) {
        refreshDistributedCache(key, "", 0, "/removeTxAbortStatusCache");
    }

    public void refreshDistributedCache(String cacheKey, String cacheValue, int expire, String restApi) {
        try {
            if (serviceList != null && !serviceList.isEmpty()) {
                String currentHostPort = InetAddress.getLocalHost().getHostName() + ":" + serverPort;
                Set<String> ipPortSet = new HashSet<>();
                serviceList.forEach(ipPort -> {
                    try {
                        if (!ipPortSet.contains(ipPort)) {
                            ipPortSet.add(ipPort);

                            if (currentHostPort.equals(ipPort)) {
                                callLocalFunction(restApi, cacheKey, cacheValue, expire);
                            } else {
                                HttpEntity<String> entity = new HttpEntity<>(cacheKey + "," + cacheValue + "," + expire);
                                log.info("Calling http://" + ipPort + restApi);
                                restTemplate.exchange("http://" + ipPort + restApi, HttpMethod.POST, entity, Boolean.class);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to refresh distributed cache. remoteHostPort [{}], restApi [{}], key  [{}], value [{}], expire [{}].", ipPort, restApi, cacheKey, cacheValue, expire, e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to execute method 'refreshDistributedCache', restApi [{}], key [{}], value [{}], expire [{}].", restApi, cacheKey, cacheValue, expire, e);
        }
    }

    private void callLocalFunction(String function, String cacheKey, String cacheValue, int expire) {
        if ("/putConfigCache".equals(function)) {
            putLocalConfigCache(cacheKey, Boolean.valueOf(cacheValue));
        } else if ("/putTxSuspendStatusCache".equals(function)) {
            putLocalTxSuspendStatusCache(cacheKey, Boolean.valueOf(cacheValue), expire);
        } else if ("/putTxAbortStatusCache".equals(function)) {
            putLocalTxAbortStatusCache(cacheKey, Boolean.valueOf(cacheValue), expire);
        } else if ("/removeConfigCache".equals(function)) {
            removeLocalConfigCache(cacheKey);
        } else if ("/removeTxSuspendStatusCache".equals(function)) {
            removeLocalTxSuspendStatusCache(cacheKey);
        } else if ("/removeTxAbortStatusCache".equals(function)) {
            removeLocalTxAbortStatusCache(cacheKey);
        } else if ("/removeTxStatusCache".equals(function)) {
            removeLocalTxStatusCache(cacheKey);
        }
    }

    @Override
    public void putLocalConfigCache(String key, Boolean value) {
        if (key != null) {
            configCache.put(key, value);
        }
    }

    @Override
    public void putLocalTxSuspendStatusCache(String key, Boolean value, int expire) {
        txSuspendStatusCache.add(new CacheEntity(key, value, expire));
    }

    @Override
    public void putLocalTxAbortStatusCache(String key, Boolean value, int expire) {
        txAbortStatusCache.add(new CacheEntity(key, value, expire));
    }

    public void removeLocalConfigCache(String key) {
        configCache.remove(key);
        if (configCache.isEmpty()) {
            configCache.clear();
        }
    }

    public void removeLocalTxStatusCache(String key) {
        removeTxStatusCache(txSuspendStatusCache, key);
        removeTxStatusCache(txAbortStatusCache, key);
    }

    public void removeLocalTxSuspendStatusCache(String key) {
        removeTxStatusCache(txSuspendStatusCache, key);
    }

    public void removeLocalTxAbortStatusCache(String key) {
        removeTxStatusCache(txAbortStatusCache, key);
    }

    private void removeTxStatusCache(ConcurrentSkipListSet<CacheEntity> txStatusCache, String key) {
        if (key != null && txStatusCache.size() > 0) {
            Set<String> keySet = new HashSet<>();
            for (String k : key.split(TxleConstants.STRING_SEPARATOR)) {
                keySet.add(k);
            }
            Iterator<CacheEntity> iterator = txStatusCache.iterator();
            while (iterator.hasNext()) {
                CacheEntity cacheEntity = iterator.next();
                if (keySet.contains(cacheEntity.getKey())) {
                    txStatusCache.remove(cacheEntity);
                }
            }
            if (txStatusCache.isEmpty()) {
                txStatusCache.clear();
            }
            keySet.clear();
        }
    }

    @PostConstruct
    void init() {
        scheduler.scheduleWithFixedDelay(() -> {
//            removeExpiredCache(txSuspendStatusCache);
//            removeExpiredCache(txAbortStatusCache);
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void removeExpiredCache(ConcurrentSkipListSet<CacheEntity> cache) {
        Iterator<CacheEntity> iterator = cache.iterator();
        // Use LinkedList due to remove frequently later.
        List<CacheEntity> removeKeys = new LinkedList<>();
        while (iterator.hasNext()) {
            CacheEntity cacheEntity = iterator.next();
            if (cacheEntity.expired()) {
                removeKeys.add(cacheEntity);
            } else {
                break;
            }
        }
        if (!removeKeys.isEmpty()) {
            removeKeys.forEach(cacheEntity -> cache.remove(cacheEntity));
            if (cache.isEmpty()) {
                cache.clear();
            }
        }
    }

    // todo ConsulServerList
    // TODO New servers need synchronized cache from the leader server when they start.
    // Notify all servers to reload the cache of service list from Consul.
    @Override
    public void refreshServiceListCache(boolean refreshRemoteServiceList) {
        try {
            Response<Map<String, Service>> agentServices = consulClient.getAgentServices();
            if (agentServices != null) {
                Map<String, Service> serviceMap = agentServices.getValue();
                if (serviceMap != null && !serviceMap.isEmpty()) {
                    Set<String> ipPortSet = new HashSet<>();
                    serviceList.clear();
                    String currentHostPort = InetAddress.getLocalHost().getHostName() + ":" + serverPort;
                    serviceMap.keySet().forEach(key -> {
                        String ipPort = "";
                        try {
                            Service service = serviceMap.get(key);
                            ipPort = service.getAddress() + ":" + service.getPort();
                            serviceList.add(ipPort);

                            if (refreshRemoteServiceList && !currentHostPort.equals(ipPort) && !ipPortSet.contains(ipPort)) {
                                ipPortSet.add(ipPort);

                                log.info("Calling http://" + ipPort + "/refreshServiceListCache refreshRemoteServiceList [{}].", refreshRemoteServiceList);
                                restTemplate.getForObject("http://" + ipPort + "/refreshServiceListCache", Boolean.TYPE);
                            }
                        } catch (Exception e) {
                            log.error("Failed to call remote method 'refreshServiceListCache', remoteHostPort [{}], refreshRemoteServiceList [{}].", ipPort, refreshRemoteServiceList, e);
                        }
                    });
                }
            }
            serviceList.forEach(s -> log.info("List of surviving services on Consul: " + s));
        } catch (Exception e) {
            log.error("Failed to call remote method 'refreshServiceListCache', refreshRemoteServiceList [{}].", refreshRemoteServiceList, e);
        }
    }

    @Override
    public void synchronizeCacheFromLeader(String consulSessionId) {
        Response<List<Session>> sessionList = consulClient.getSessionList(null);
        if (sessionList != null) {
            List<Session> sessions = sessionList.getValue();
            if (sessions != null && sessions.size() > 1) {
                sessions.forEach(session -> {
                    Response<Map<String, Service>> agentServices = consulClient.getAgentServices();
                    if (agentServices != null) {
                        Map<String, Service> serviceMap = agentServices.getValue();
                        if (serviceMap != null && !serviceMap.isEmpty()) {
                            serviceMap.keySet().forEach(key -> {
                                Service service = serviceMap.get(key);
                                String ipPort = service.getAddress() + ":" + service.getPort();
                            });
                        }
                    }
                    if (!consulSessionId.equals(session.getId()) && consulClient.setKVValue(CONSUL_LEADER_KEY + "?acquire=" + session.getId(), CONSUL_LEADER_KEY_VALUE).getValue()) {
                        // the leader node
//                        session.get
//                        restTemplate.getForObject("http://" + ipPort + "/refreshServiceListCache", Boolean.TYPE);
                    }
                });
            }
        }
    }
}