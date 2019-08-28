/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * 该类为系统静态配置类，主要加载*.yml,*.yaml和*.properties文件中的配置，并将其转换成键值对形式存储于静态属性中，供整个系统调用
 *
 * @author Gannalyo
 * @since 2019/4/16
 */
public final class TxleStaticConfig {
    private static final Map<String, Object> TXLE_STATIC_CONFIG = new HashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(TxleStaticConfig.class);

    private TxleStaticConfig() {
    }

    public static void initTxleStaticConfig() {
        try {
            File resourceDir = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX);
            if (resourceDir != null && resourceDir.isDirectory()) {
                File[] files = resourceDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                            convertYmlToMapConfig(file.getPath());
                        } else if (file.getName().endsWith(".properties")) {
                            convertPropertiesToMapConfig(file.getPath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to initialize the Static Configs.", e);
        }
    }

    private static void convertYmlToMapConfig(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            convertYmlToMapConfig(new Yaml().loadAs(inputStream, HashMap.class), null);
        } catch (FileNotFoundException e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to initialize the Static Configs - filePath = " + filePath, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to close stream - filePath = " + filePath, e);
                }
            }
        }
    }

    private static void convertYmlToMapConfig(Map<String, Object> map, String keyPath) {
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            try {
                if (keyPath == null) {
                    keyPath = entry.getKey();
                } else {
                    keyPath = keyPath + "." + entry.getKey();
                }

                Object value = map.get(entry.getKey());
                if (value instanceof Map) {
                    convertYmlToMapConfig((Map) value, keyPath);
                } else {
                    TXLE_STATIC_CONFIG.put(keyPath, value);
                }

                int lastKeyIndex = keyPath.lastIndexOf(".");
                if (lastKeyIndex > 0) {
                    keyPath = keyPath.substring(0, lastKeyIndex);
                } else {
                    keyPath = null;
                }
            } catch (Exception e) {
                LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to initialize the Static Configs - key = " + entry.getKey(), e);
            }
        }
    }

    public static void convertPropertiesToMapConfig(String filePath) {
        InputStream inputStream = null;
        try {
            Properties properties = new Properties();
            inputStream = new FileInputStream(filePath);
            properties.load(inputStream);
            if (properties.isEmpty()) {
                LOG.info("No property in [{}].", filePath);
            }
            properties.keySet().forEach(key -> TXLE_STATIC_CONFIG.put(key + "", properties.get(key)));
        } catch (IOException e) {
            LOG.error("Failed to initialize the Static Configs - filePath = [{}]", filePath, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.error("Failed to close input stream - filePath = [{}]", filePath, e);
                }
            }
        }
    }

    public static String getStringConfig(String configFullName, String defaultValue) {
        Object value = TXLE_STATIC_CONFIG.get(configFullName);
        if (value == null) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    public static Integer getIntegerConfig(String configFullName, Integer defaultValue) {
        Object value = TXLE_STATIC_CONFIG.get(configFullName);
        try {
            if (value != null) {
                return Integer.valueOf(value.toString().trim());
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public static Boolean getBooleanConfig(String configFullName, Boolean defaultValue) {
        Object value = TXLE_STATIC_CONFIG.get(configFullName);
        try {
            if (value != null) {
                return Boolean.valueOf(value.toString().trim());
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public static Object getConfig(String configFullName) {
        return TXLE_STATIC_CONFIG.get(configFullName);
    }

}
