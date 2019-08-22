package org.apache.servicecomb.saga.alpha.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ConfigLoading {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoading.class);

    private ConfigLoading() {
    }

    public static Map<String, Object> loadKafkaProperties() {
        InputStream inputStream = null;
        try {
            Properties properties = new Properties();
            // PropertiesLoaderUtils.loadAllProperties("kafka.properties");
            // properties.load(System.class.getResourceAsStream("kafka.properties"));
            // It will load the cache file for 'kafka.properties' by the two ways above.
            // It will load the realistic kafka.properties.
            inputStream = new FileInputStream(System.class.getResource("/kafka.properties").getPath());
            properties.load(inputStream);
            if (properties.isEmpty()) {
                LOG.info("No property in kafka.properties.");
            }
            Map<String, Object> kafkaConfigMap = new HashMap<>();
            properties.keySet().forEach(key -> {
                kafkaConfigMap.put(key + "", properties.get(key));
            });
            return kafkaConfigMap;
        } catch (IOException e) {
            LOG.error("Failed to load the kafka.properties file.", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.error("Failed to close the stream of the file 'kafka.properties'.", e);
                }
            }
        }
        return new HashMap<>();
    }
}
