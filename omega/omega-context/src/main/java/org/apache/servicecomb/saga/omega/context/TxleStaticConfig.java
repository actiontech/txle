package org.apache.servicecomb.saga.omega.context;

import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 该类为系统静态配置类，主要加载*.yml,*.yaml和*.properties文件中的配置，并将其转换成键值对形式存储于静态属性中，供整个系统调用
 *
 * @author Gannalyo
 * @date 2019/4/16
 */
public class TxleStaticConfig {
    private static final Map<String, Object> txleStaticConfig = new HashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(TxleStaticConfig.class);

    public TxleStaticConfig() {
        try {
            File resourceDir = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX);
            if (resourceDir != null && resourceDir.isDirectory()) {
                for (File file : resourceDir.listFiles()) {
                    if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                        convertYmlToMapConfig(file.getPath());
                    } else if (file.getName().endsWith(".properties")) {
                        convertPropertiesToMapConfig(file.getPath());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to initialize the Static Configs.", e);
        }
    }

    private static void convertYmlToMapConfig(String filePath) {
        try {
            convertYmlToMapConfig(new Yaml().loadAs(new FileInputStream(filePath), HashMap.class), null);
        } catch (FileNotFoundException e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to initialize the Static Configs - filePath = " + filePath, e);
        }
    }

    private static void convertYmlToMapConfig(Map<String, Object> map, String keyPath) {
        for (String key : map.keySet()) {
            try {
                if (keyPath == null) {
                    keyPath = key;
                } else {
                    keyPath = keyPath + "." + key;
                }

                Object value = map.get(key);
                if (value instanceof Map) {
                    convertYmlToMapConfig((Map) value, keyPath);
                } else {
                    txleStaticConfig.put(keyPath, value);
                }

                int lastKeyIndex = keyPath.lastIndexOf(".");
                if (lastKeyIndex > 0) {
                    keyPath = keyPath.substring(0, lastKeyIndex);
                } else {
                    keyPath = null;
                }
            } catch (Exception e) {
                LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to initialize the Static Configs - key = " + key, e);
            }
        }
    }

    public static void convertPropertiesToMapConfig(String filePath) {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(filePath));
            if (properties.isEmpty()) {
                LOG.info("No property in [{}].", filePath);
            }
            properties.keySet().forEach(key -> {
                txleStaticConfig.put(key + "", properties.get(key));
            });
        } catch (IOException e) {
            LOG.error("Failed to initialize the Static Configs - filePath = [{}]", filePath, e);
        }
    }

    public static String getStringConfig(String configFullName, String defaultValue) {
        Object value = txleStaticConfig.get(configFullName);
        if (value == null) return defaultValue;
        return value.toString().trim();
    }

    public static Integer getIntegerConfig(String configFullName, Integer defaultValue) {
        Object value = txleStaticConfig.get(configFullName);
        try {
            if (value != null) {
                return Integer.valueOf(value.toString().trim());
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public static Boolean getBooleanConfig(String configFullName, Boolean defaultValue) {
        Object value = txleStaticConfig.get(configFullName);
        try {
            if (value != null) {
                return Boolean.valueOf(value.toString().trim());
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public static Object getConfig(String configFullName) {
        return txleStaticConfig.get(configFullName);
    }

}
