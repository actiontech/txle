/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.CallableStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.PreparedStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.ResultSetInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.listener.JdbcEventListener;
import org.apache.servicecomb.saga.omega.transaction.monitor.CompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class DefaultJdbcEventListener extends JdbcEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultJdbcEventListener.class);

    @Override
    public void onBeforeGetConnection() {
    }

    @Override
    public void onAfterGetConnection(SQLException e) {
    }

    @Override
    @Deprecated
    public void onConnectionWrapped() {
    }

    @Override
    public void onBeforeAddBatch(PreparedStatementInformation statementInformation) {
    }

    @Override
    public void onAfterAddBatch(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onBeforeAddBatch(String sql) {
    }

    @Override
    public void onAfterAddBatch(long timeElapsedNanos, String sql, SQLException e) {
    }

    @Override
    public void onBeforeExecute(PreparedStatementInformation statementInformation) {
    }

    @Override
    public void onAfterExecute(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onBeforeExecute(String sql) {
    }

    @Override
    public void onAfterExecute(long timeElapsedNanos, String sql, SQLException e) {
    }

    @Override
    public void onBeforeExecuteBatch() {
    }

    @Override
    public void onAfterExecuteBatch(long timeElapsedNanos, int[] updateCounts, SQLException e) {
    }

    // The Aspect annotation works for Spring Bean only By Gannalyo
    @Override
    public void onBeforeExecuteUpdate(PreparedStatementInformation preparedStatementInformation) {
        LOG.info(this.getClass() + " - onBeforeExecuteUpdate(PreparedStatementInformation statementInformation).");
    }

    @Override
    public void onBeforeExecuteUpdate(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation) {
        LOG.info(this.getClass() + " - onBeforeExecuteUpdate(PreparedStatementInformation statementInformation).");
        if (!CurrentThreadOmegaContext.isAutoCompensate()) {
            // start to mark duration for business sql By Gannalyo.
            ApplicationContextUtil.getApplicationContext().getBean(CompensableSqlMetrics.class).startMarkSQLDurationAndCount(preparedStatementInformation.getSqlWithValues(), true);
        }
    }

    @Override
    public Object onBeforeExecuteUpdateWithReturnValue(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation) throws SQLException {
        try {
//            if (!CurrentThreadOmegaContext.isAutoCompensate()) {
//                // start to mark duration for business sql By Gannalyo.
//                ApplicationContextUtil.getApplicationContext().getBean(CompensableSqlMetrics.class).startMarkSQLDurationAndCount(preparedStatementInformation.getSqlWithValues(), true);
//            }
            return null;
        } catch (Exception e) {
            LOG.error("Failed to execute method " + this.getClass() + " - onBeforeExecuteUpdate(PreparedStatementInformation statementInformation).", e);
        }
        return null;
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation) {
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos,
                                     int rowCount, SQLException e) {
        LOG.info(this.getClass() + " - onAfterExecuteUpdate(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, int rowCount, SQLException e).");
    }

    @Override
    public void onAfterExecuteUpdateWithParams(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos,
                                               int rowCount, SQLException e, Map<JdbcEventListener, Object> listenerParams) {
        try {
            if (!CurrentThreadOmegaContext.isAutoCompensate()) {
                // end mark duration for business sql By Gannalyo.
                ApplicationContextUtil.getApplicationContext().getBean(CompensableSqlMetrics.class).endMarkSQLDuration();
            }
        } catch (Exception ex) {
            LOG.error("Failed to execute method " + this.getClass() + " - onAfterExecuteUpdateWithParams(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, int rowCount, SQLException e, Object params).", ex);
        }
    }

    @Override
    public void onBeforeExecuteUpdate(String sql) {
        LOG.info(this.getClass() + " - onBeforeExecuteUpdate(String sql).");
    }

    @Override
    public void onAfterExecuteUpdate(long timeElapsedNanos, String sql, int rowCount, SQLException e) {
        LOG.info(this.getClass() + " - onAfterExecuteUpdate(long timeElapsedNanos, String sql, int rowCount, SQLException e).");
    }

    @Override
    public void onBeforeExecuteQuery(PreparedStatementInformation statementInformation) {
        LOG.info(this.getClass() + " - onBeforeExecuteQuery(PreparedStatementInformation statementInformation).");
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, SQLException e) {
        LOG.info(this.getClass() + " - onAfterExecuteQuery(PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos, SQLException e).");
    }

    @Override
    public void onBeforeExecuteQuery(String sql) {
        LOG.info(this.getClass() + " - onBeforeExecuteQuery(String sql).");
    }

    @Override
    public void onAfterExecuteQuery(long timeElapsedNanos, String sql, SQLException e) {
        LOG.info(this.getClass() + " - onAfterExecuteQuery(long timeElapsedNanos, String sql, SQLException e).");
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
    }

    @Override
    public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) {
    }

    @Override
    public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e) {
    }

    @Override
    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value, SQLException e) {
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value, SQLException e) {
    }

    @Override
    public void onBeforeCommit() {
    }

    @Override
    public void onAfterCommit(long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onAfterConnectionClose(SQLException e) {
    }

    @Override
    public void onBeforeRollback() {
    }

    @Override
    public void onAfterRollback(long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onAfterStatementClose(SQLException e) {
    }

}
