/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache tool for localTxId and DataSource.
 * @author Gannalyo
 * @since 2018-12-03
 */
public final class DataSourceMappingCache {

    private static final Map<String, DataSource> LOCAL_TX_ID_AND_DATA_SOURCE = new ConcurrentHashMap<>();

    private DataSourceMappingCache() {
    }

    public static void putLocalTxIdAndDataSourceInfo(String localTxId, String url, String userName, String driverName) {
        if (LOCAL_TX_ID_AND_DATA_SOURCE.get(localTxId) == null) {
            ApplicationContext applicationContext = ApplicationContextUtil.getApplicationContext();
            Map<String, DataSource> dataSourceMap = applicationContext.getBeansOfType(DataSource.class);
            if (dataSourceMap != null && !dataSourceMap.isEmpty()) {
                synchronized (DataSourceMappingCache.class) {
                    Iterator<Map.Entry<String, DataSource>> iterator = dataSourceMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Connection connection = null;
                        try {
                            DataSource dataSource = iterator.next().getValue();
                            connection = dataSource.getConnection();
                            DatabaseMetaData databaseMetaData = connection.getMetaData();
                            if (databaseMetaData.getURL().equals(url) && databaseMetaData.getUserName().equals(userName) && databaseMetaData.getDriverName().equals(driverName)) {
                                LOCAL_TX_ID_AND_DATA_SOURCE.put(localTxId, dataSource);
                                break;
                            }
                        } catch (SQLException e) {
                        } finally {
                            if (connection != null) {
                                try {
                                    connection.close();
                                } catch (SQLException e) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static DataSource get(String localTxId) {
        return LOCAL_TX_ID_AND_DATA_SOURCE.get(localTxId);
    }

    public static Set<String> getCacheLocalTxIdSet() {
        return LOCAL_TX_ID_AND_DATA_SOURCE.keySet();
    }

    public static void clear(Set<String> localTxIdOfEndedGlobalTx) {
        if (localTxIdOfEndedGlobalTx != null) {
            localTxIdOfEndedGlobalTx.forEach(key -> LOCAL_TX_ID_AND_DATA_SOURCE.remove(key));
        }
    }
}
