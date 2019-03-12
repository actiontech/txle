package org.apache.servicecomb.saga.alpha.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigLoading {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoading.class);

    public static Map<String, Object> loadKafkaProperties() {
        try {
            Properties properties = new Properties();
            // PropertiesLoaderUtils.loadAllProperties("kafka.properties");
            // properties.load(System.class.getResourceAsStream("kafka.properties"));
            // It will load the cache file for 'kafka.properties' by the two ways above.
            // It will load the realistic kafka.properties.
            properties.load(new FileInputStream(System.class.getResource("/kafka.properties").getPath()));
            if (properties.isEmpty()) {
                LOG.info("No property in kafka.properties.");
            }
            Map<String, Object> kafkaConfigMap = new HashMap<>();
            properties.keySet().forEach(key -> {
                kafkaConfigMap.put(key + "", properties.get(key));
            });
            return kafkaConfigMap;
        } catch (IOException e) {
            LOG.error("Failed to load the kafka.properties file." + e);
        }
        return new HashMap<>();
    }
}
