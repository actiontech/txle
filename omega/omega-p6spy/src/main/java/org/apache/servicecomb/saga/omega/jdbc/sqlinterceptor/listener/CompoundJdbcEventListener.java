/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.listener;

import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.CallableStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.PreparedStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.ResultSetInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * To define compound event listener for storing and invoking different event listeners only.
 *
 * @author Gannalyo
 * @since 20190129
 */
public class CompoundJdbcEventListener extends JdbcEventListener {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<JdbcEventListener> eventListeners;

    public CompoundJdbcEventListener() {
        eventListeners = new ArrayList<>();
    }

    public void addListender(JdbcEventListener listener) {
        eventListeners.add(listener);
    }

    @Override
    public void onBeforeGetConnection() {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeGetConnection();
        }
    }

    @Override
    public void onAfterGetConnection(SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterGetConnection(e);
        }
    }

    @Override
    @Deprecated
    public void onConnectionWrapped() {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onConnectionWrapped();
        }
    }

    @Override
    public void onBeforeAddBatch(PreparedStatementInformation statementInformation) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeAddBatch(statementInformation);
        }
    }

    @Override
    public void onAfterAddBatch(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterAddBatch(preparedStatementInformation, timeElapsedNanos, e);
        }
    }

    @Override
    public void onBeforeAddBatch(String sql) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeAddBatch(sql);
        }
    }

    @Override
    public void onAfterAddBatch(long timeElapsedNanos, String sql, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterAddBatch(timeElapsedNanos, sql, e);
        }
    }

    @Override
    public void onBeforeExecute(PreparedStatementInformation statementInformation) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecute(statementInformation);
        }
    }

    @Override
    public void onBeforeExecuteUpdate(PreparedStatement preparedStatement, PreparedStatementInformation statementInformation) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecuteUpdate(preparedStatement, statementInformation);
        }
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatement preparedStatement, PreparedStatementInformation statementInformation) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecuteUpdate(preparedStatement, statementInformation);
        }
    }

    @Override
    public Object onBeforeExecuteUpdateWithReturnValue(PreparedStatement preparedStatement, PreparedStatementInformation statementInformation) throws SQLException {
        Map<JdbcEventListener, Object> listenerParams = new ConcurrentHashMap<>();
        for (JdbcEventListener eventListener : eventListeners) {
            listenerParams.put(eventListener, eventListener.onBeforeExecuteUpdateWithReturnValue(preparedStatement, statementInformation));
        }
        return listenerParams;
    }

    @Override
    public void onAfterExecute(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecute(preparedStatementInformation, timeElapsedNanos, e);
        }
    }

    @Override
    public void onBeforeExecute(String sql) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecute(sql);
        }
    }

    @Override
    public void onAfterExecute(long timeElapsedNanos, String sql, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecute(timeElapsedNanos, sql, e);
        }
    }

    @Override
    public void onBeforeExecuteBatch() {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecuteBatch();
        }
    }

    @Override
    public void onAfterExecuteBatch(long timeElapsedNanos, int[] updateCounts, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecuteBatch(timeElapsedNanos, updateCounts, e);
        }
    }

    // The Aspect annotation works for Spring Bean only By Gannalyo
    @Override
    public void onBeforeExecuteUpdate(PreparedStatementInformation statementInformation) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecuteUpdate(statementInformation);
        }
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos,
                                     int rowCount, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecuteUpdate(preparedStatementInformation, timeElapsedNanos, rowCount, e);
        }
    }

    @Override
    public void onAfterExecuteUpdateWithParams(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos,
                                               int rowCount, SQLException e, Map<JdbcEventListener, Object> listenerParams) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecuteUpdateWithParams(preparedStatement, preparedStatementInformation, timeElapsedNanos, rowCount, e, listenerParams);
        }
    }

    @Override
    public void onBeforeExecuteUpdate(String sql) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecuteUpdate(sql);
        }
    }

    @Override
    public void onAfterExecuteUpdate(long timeElapsedNanos, String sql, int rowCount, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecuteUpdate(timeElapsedNanos, sql, rowCount, e);
        }
    }

    @Override
    public void onBeforeExecuteQuery(PreparedStatementInformation statementInformation) {
        log.debug("Executing method 'onBeforeExecuteQuery' ....");
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecuteQuery(statementInformation);
        }
        log.debug("Executed method 'onBeforeExecuteQuery' ....");
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecuteQuery(preparedStatementInformation, timeElapsedNanos, e);
        }
    }

    @Override
    public void onBeforeExecuteQuery(String sql) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeExecuteQuery(sql);
        }
    }

    @Override
    public void onAfterExecuteQuery(long timeElapsedNanos, String sql, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterExecuteQuery(timeElapsedNanos, sql, e);
        }
    }

    @Override
    public void onAfterPreparedStatementSet(PreparedStatementInformation preparedStatementInformation, int parameterIndex, Object value, SQLException e) {
        preparedStatementInformation.setParameterValue(parameterIndex, value);
    }

    @Override
    public void onAfterCallableStatementSet(CallableStatementInformation callableStatementInformation, String parameterName, Object value, SQLException e) {
        callableStatementInformation.setParameterValue(parameterName, value);
    }

    @Override
    public void onAfterGetResultSet(long timeElapsedNanos, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterGetResultSet(timeElapsedNanos, e);
        }
    }

    @Override
    public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeResultSetNext(resultSetInformation);
        }
    }

    @Override
    public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterResultSetNext(resultSetInformation, timeElapsedNanos, hasNext, e);
        }
    }

    @Override
    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterResultSetClose(resultSetInformation, e);
        }
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterResultSetGet(resultSetInformation, columnLabel, value, e);
        }
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterResultSetGet(resultSetInformation, columnIndex, value, e);
        }
    }

    @Override
    public void onBeforeCommit() {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeCommit();
        }
    }

    @Override
    public void onAfterCommit(long timeElapsedNanos, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterCommit(timeElapsedNanos, e);
        }
    }

    @Override
    public void onAfterConnectionClose(SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterConnectionClose(e);
        }
    }

    @Override
    public void onBeforeRollback() {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onBeforeRollback();
        }
    }

    @Override
    public void onAfterRollback(long timeElapsedNanos, SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterRollback(timeElapsedNanos, e);
        }
    }

    @Override
    public void onAfterStatementClose(SQLException e) {
        for (JdbcEventListener eventListener : eventListeners) {
            eventListener.onAfterStatementClose(e);
        }
    }
}
