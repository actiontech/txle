package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.wrapper;

import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.listener.CompoundJdbcEventListener;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.listener.JdbcEventListener;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * To wrap the database driver.
 *
 * @author Gannalyo
 * @since 20190129
 */
public class Driver implements java.sql.Driver {

    @Override
    public boolean acceptsURL(final String url) {
        // The purpose of using this prefix is to avoid modifying the application's url.
        return url != null && url.startsWith("jdbc:");
    }

    // To store a global list for drivers.
    private static final List<java.sql.Driver> DRIVER_LIST = collectRegisteredDrivers();

    /**
     * To collect all drivers which come from 'java.sql.DriverManager.getDrivers()'.
     *
     * @return drivers.
     */
    static List<java.sql.Driver> collectRegisteredDrivers() {
        List<java.sql.Driver> driverList = new ArrayList<>();
        for (Enumeration<java.sql.Driver> driverEnumeration = DriverManager.getDrivers(); driverEnumeration.hasMoreElements(); ) {
            driverList.add(driverEnumeration.nextElement());
        }
        return driverList;
    }

    @Override
    public Connection connect(String url, Properties properties) throws SQLException {
        if (url == null || url.trim().length() == 0) {
            throw new SQLException("Failed to load database driver, the url is required.");
        }

        if (!acceptsURL(url)) {
            throw new SQLException("Failed to load database driver, the url should start with 'jdbc:'.");
        }

        // find the real driver for the URL
        java.sql.Driver passThru = findPassthru(url);
        final Connection conn;
        try {
            conn = passThru.connect(url, properties);
        } catch (SQLException e) {
            throw e;
        }

        // 'CompoundJdbcEventListener' is used as a collection for storing different 'JdbcEventListener', and some method will be invoked by all 'JdbcEventListener' in this collection.
        CompoundJdbcEventListener compoundJdbcEventListener = new CompoundJdbcEventListener();
        ServiceLoader<JdbcEventListener> listeners = ServiceLoader.load(JdbcEventListener.class);
        if (listeners != null) {
            for (JdbcEventListener listener : listeners) {
                compoundJdbcEventListener.addListender(listener);
            }
        }

        return new ConnectionWrapper(conn, compoundJdbcEventListener);
    }

    protected java.sql.Driver findPassthru(String url) throws SQLException {
        java.sql.Driver passthru = null;
        for (java.sql.Driver driver : DRIVER_LIST) {
            try {
                if (driver.acceptsURL(url)) {
                    passthru = driver;
                    break;
                }
            } catch (SQLException e) {
            }
        }
        if (passthru == null) {
            throw new SQLException("Unable to find a driver that accepts " + url);
        }
        return passthru;
    }


    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException {
        return findPassthru(url).getPropertyInfo(url, properties);
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    // Note: @Override annotation not added to allow compilation using Java 1.6
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
    }
}
