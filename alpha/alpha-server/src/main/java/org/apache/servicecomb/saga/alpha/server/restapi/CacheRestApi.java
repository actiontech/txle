package org.apache.servicecomb.saga.alpha.server.restapi;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CacheRestApi {
    public static final ConcurrentHashMap<String, Boolean> enabledConfigMap = new ConcurrentHashMap<>();

    @GetMapping("/clearConfigCache")
    public String clearConfigCache() {
        enabledConfigMap.clear();
        return HttpStatus.OK.toString();
    }

    public static String clearByKey(String key) {
        enabledConfigMap.remove(key);
        if (enabledConfigMap.isEmpty()) {
            enabledConfigMap.clear();// 释放内存
        }
        return HttpStatus.OK.toString();
    }

}
