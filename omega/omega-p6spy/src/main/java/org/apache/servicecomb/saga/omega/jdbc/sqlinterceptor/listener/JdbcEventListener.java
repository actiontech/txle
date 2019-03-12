package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.listener;

import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.CallableStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.PreparedStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.ResultSetInformation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * To define event listener for executing jdbc operations.
 *
 * @author Gannalyo
 * @date 20190129
 */
public abstract class JdbcEventListener {

    public void onBeforeGetConnection() {
    }

    public void onAfterGetConnection(SQLException e) {
    }

    public void onConnectionWrapped() {
    }

    public void onBeforeAddBatch(PreparedStatementInformation statementInformation) {
    }

    public void onAfterAddBatch(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    }

    public void onBeforeAddBatch(String sql) {
    }

    public void onAfterAddBatch(long timeElapsedNanos, String sql, SQLException e) {
    }

    public void onBeforeExecute(PreparedStatementInformation statementInformation) {
    }

    public void onAfterExecute(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    }

    public void onBeforeExecute(String sql) {
    }

    public void onAfterExecute(long timeElapsedNanos, String sql, SQLException e) {
    }

    public void onBeforeExecuteBatch() {
    }

    public void onAfterExecuteBatch(long timeElapsedNanos, int[] updateCounts, SQLException e) {
    }

    public void onBeforeExecuteUpdate(PreparedStatementInformation statementInformation) {
    }

    public void onBeforeExecuteUpdate(PreparedStatement preparedStatement, PreparedStatementInformation statementInformation) {
    }

    public Object onBeforeExecuteUpdateWithReturnValue(PreparedStatement preparedStatement, PreparedStatementInformation statementInformation) throws SQLException {
        return null;
    }

    public void onAfterExecuteUpdate(PreparedStatement preparedStatement, PreparedStatementInformation statementInformation) {
    }

    public void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e) {
    }

    public void onAfterExecuteUpdateWithParams(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation, long timeElapsedNanos,
                                               int rowCount, SQLException e, Map<JdbcEventListener, Object> listenerParams) {
    }

    public void onBeforeExecuteUpdate(String sql) {
    }

    public void onAfterExecuteUpdate(long timeElapsedNanos, String sql, int rowCount, SQLException e) {
    }

    public void onBeforeExecuteQuery(PreparedStatementInformation statementInformation) {
    }

    public void onAfterExecuteQuery(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    }

    public void onBeforeExecuteQuery(String sql) {
    }

    public void onAfterExecuteQuery(long timeElapsedNanos, String sql, SQLException e) {
    }

    public void onAfterPreparedStatementSet(PreparedStatementInformation statementInformation, int parameterIndex, Object value, SQLException e) {
    }

    public void onAfterCallableStatementSet(CallableStatementInformation statementInformation, String parameterName, Object value, SQLException e) {
    }

    public void onAfterGetResultSet(long timeElapsedNanos, SQLException e) {
    }

    public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) {
    }

    public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e) {
    }

    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
    }

    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value, SQLException e) {
    }

    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value, SQLException e) {
    }

    public void onBeforeCommit() {
    }

    public void onAfterCommit(long timeElapsedNanos, SQLException e) {
    }

    public void onAfterConnectionClose(SQLException e) {
    }

    public void onBeforeRollback() {
    }

    public void onAfterRollback(long timeElapsedNanos, SQLException e) {
    }

    public void onAfterStatementClose(SQLException e) {
    }

}
