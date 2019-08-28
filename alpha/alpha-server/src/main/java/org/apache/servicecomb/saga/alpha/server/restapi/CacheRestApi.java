package org.apache.servicecomb.saga.alpha.server.restapi;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CacheRestApi {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ConcurrentHashMap<String, Boolean> configCacheMap = new ConcurrentHashMap<>();

    @Autowired
    private ConsulClient consulClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.server.port:8090}")
    private int serverPort;

    public Boolean get(String key) {
        return configCacheMap.get(key);
    }

    public Enumeration<String> keys() {
        return configCacheMap.keys();
    }

    public void putForDistributedCache(String key, Boolean value) {
        refreshDistributedConfigCache(key, value, "/putConfigCache");
    }

    public void removeForDistributedCache(String key) {
        refreshDistributedConfigCache(key, null, "/removeConfigCache");
    }

    @PostMapping("/putConfigCache")
    public void putConfigCache(@RequestBody String cacheKV) {
        if (cacheKV != null) {
            String[] arrKV = cacheKV.split(",");
            configCacheMap.put(arrKV[0], Boolean.valueOf(arrKV[1]));
        }
    }

    @PostMapping("/removeConfigCache")
    public void removeConfigCache(@RequestBody String cacheKV) {
        if (cacheKV != null) {
            configCacheMap.remove(cacheKV.split(",")[0]);
            if (configCacheMap.isEmpty()) {
                configCacheMap.clear();
            }
        }
    }

    public void refreshDistributedConfigCache(String cacheKey, Boolean cacheValue, String restApi) {
        try {
            Response<Map<String, Service>> agentServices = consulClient.getAgentServices();
            if (agentServices != null) {
                Map<String, Service> serviceMap = agentServices.getValue();
                if (serviceMap != null && !serviceMap.isEmpty()) {
                    String currentHostPort = InetAddress.getLocalHost().getHostName() + ":" + serverPort;
                    Set<String> ipPortSet = new HashSet<>();
                    serviceMap.keySet().forEach(key -> {
                        Service service = serviceMap.get(key);
                        String ipPort = service.getAddress() + ":" + service.getPort();
                        if (!ipPortSet.contains(ipPort)) {
                            ipPortSet.add(ipPort);

                            if (currentHostPort.equals(ipPort)) {
                                if ("/putConfigCache".equals(restApi)) {
                                    putConfigCache(cacheKey + "," + cacheValue);
                                } else if ("/removeConfigCache".equals(restApi)) {
                                    removeConfigCache(cacheKey + "," + cacheValue);
                                }
                            } else {
                                HttpEntity<String> entity = new HttpEntity<>(cacheKey + "," + cacheValue);
                                log.error("Calling http://" + ipPort + restApi);
                                restTemplate.exchange("http://" + ipPort + restApi, HttpMethod.POST, entity, Boolean.class);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute method 'refreshDistributedConfigCache'.", e);
        }
    }

}
