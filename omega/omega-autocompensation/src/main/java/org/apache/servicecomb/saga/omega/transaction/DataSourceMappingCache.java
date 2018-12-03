package org.apache.servicecomb.saga.omega.transaction;

import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache tool for localTxId and DataSource.
 * @author Gannalyo
 * @date 2018-12-03
 */
public class DataSourceMappingCache {

    private static final Map<String, DataSource> localTxIdAndDataSource = new ConcurrentHashMap<>();

    public static void putLocalTxIdAndDataSourceInfo(String localTxId, String url, String userName, String driverName) {
        if (localTxIdAndDataSource.get(localTxId) == null) {
            ApplicationContext applicationContext = ApplicationContextUtil.getApplicationContext();
            Map<String, DataSource> dataSourceMap = applicationContext.getBeansOfType(DataSource.class);
            if (dataSourceMap != null && !dataSourceMap.isEmpty()) {
                synchronized (DataSourceMappingCache.class) {
                    for (String key : dataSourceMap.keySet()) {
                        try {
                            DataSource dataSource = dataSourceMap.get(key);
                            DatabaseMetaData databaseMetaData = dataSource.getConnection().getMetaData();
                            if (databaseMetaData.getURL().equals(url) && databaseMetaData.getUserName().equals(userName) && databaseMetaData.getDriverName().equals(driverName)) {
                                localTxIdAndDataSource.put(localTxId, dataSource);
                                break;
                            }
                        } catch (SQLException e) {
                        }
                    }
                }
            }
        }
    }

    public static DataSource get(String localTxId) {
        return localTxIdAndDataSource.get(localTxId);
    }

    public static Set<String> getCacheLocalTxIdSet() {
        return localTxIdAndDataSource.keySet();
    }

    public static void clear(Set<String> localTxIdOfEndedGlobalTx) {
        if (localTxIdOfEndedGlobalTx != null) {
            localTxIdOfEndedGlobalTx.forEach(key -> {
                localTxIdAndDataSource.remove(key);
            });
        }
    }
}
