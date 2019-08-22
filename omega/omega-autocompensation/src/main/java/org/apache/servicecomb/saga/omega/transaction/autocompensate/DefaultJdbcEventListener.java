package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.CallableStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.PreparedStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.ResultSetInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.listener.JdbcEventListener;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.wrapper.PreparedStatementWrapper;
import org.apache.servicecomb.saga.omega.transaction.KafkaMessage;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
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
        try {
            if (CurrentThreadOmegaContext.isAutoCompensate()) {
                // before advise for executing SQL By Gannalyo.
                if (CurrentThreadOmegaContext.isEnabledAutoCompensateTx()) {
                    AutoCompensateHandler.newInstance().saveAutoCompensationInfo(preparedStatement, preparedStatementInformation.getSqlWithValues(), true, null);
                }

                // start to mark duration for business sql By Gannalyo.
                ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(preparedStatementInformation.getSqlWithValues(), true);
            }
        } catch (Exception e) {
            LOG.error("Failed to execute method " + this.getClass() + " - onBeforeExecuteUpdate(PreparedStatementInformation statementInformation).", e);
        }
    }

    @Override
    public Object onBeforeExecuteUpdateWithReturnValue(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation) throws SQLException {
        try {
            if (CurrentThreadOmegaContext.isAutoCompensate()) {
                // before advise for executing SQL By Gannalyo.
                Map<String, Object> standbyParams = new HashMap<>();
                if (CurrentThreadOmegaContext.isEnabledAutoCompensateTx()) {
                    AutoCompensateHandler.newInstance().saveAutoCompensationInfo(preparedStatement, preparedStatementInformation.getSqlWithValues(), true, standbyParams);
                }

                // start to mark duration for business sql By Gannalyo.
                ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).startMarkSQLDurationAndCount(preparedStatementInformation.getSqlWithValues(), true);

                return standbyParams;
            }
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
            // after advise for executing SQL By Gannalyo.
            if (CurrentThreadOmegaContext.isAutoCompensate()) {
                // end mark duration for business sql By Gannalyo.
                ApplicationContextUtil.getApplicationContext().getBean(AutoCompensableSqlMetrics.class).endMarkSQLDuration();

                if (CurrentThreadOmegaContext.isEnabledAutoCompensateTx()) {
                    Map<String, Object> standbyParams = null;
                    if (listenerParams != null) {
                        Object params = listenerParams.get(this);
                        if (params != null) {
                            standbyParams = (Map<String, Object>) params;
                        }
                    }
                    if (standbyParams == null) {
                        standbyParams = new HashMap<>();
                    }

                    AutoCompensateHandler.newInstance().saveAutoCompensationInfo(preparedStatement, preparedStatementInformation.getSqlWithValues(), false, standbyParams);

                    // To construct business information, and then report to the txle Server.
                    constructBusinessInfoToServer(standbyParams);
                }
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

    // To construct business information, and then report to the txle Server.
    private void constructBusinessInfoToServer(Map<String, Object> standbyParams) {
        final Logger log = LoggerFactory.getLogger(PreparedStatementWrapper.class);
        try {
            if (standbyParams == null || standbyParams.isEmpty()) {
                return;
            }

            String dbdrivername = String.valueOf(standbyParams.get("dbdrivername"));
            String dburl = String.valueOf(standbyParams.get("dburl"));
            String dbusername = String.valueOf(standbyParams.get("dbusername"));

            String tableName = String.valueOf(standbyParams.get("tablename"));
            String operation = String.valueOf(standbyParams.get("operation"));
            String ids = String.valueOf(standbyParams.get("ids"));

            String globalTxId = CurrentThreadOmegaContext.getGlobalTxIdFromCurThread();
            String localTxId = CurrentThreadOmegaContext.getLocalTxIdFromCurThread();

            MessageSender messageSender = ApplicationContextUtil.getApplicationContext().getBean(MessageSender.class);
            messageSender.reportMessageToServer(new KafkaMessage(globalTxId, localTxId, dbdrivername, dburl, dbusername, tableName, operation, ids));
        } catch (Exception e) {
            log.error("Failed to execute the method 'constructBusinessInfoToServer'.", e);
        }
    }
}
