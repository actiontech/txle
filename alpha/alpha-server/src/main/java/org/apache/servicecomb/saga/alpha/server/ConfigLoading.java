/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

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
