/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.wrapper;

import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.PreparedStatementInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info.ResultSetInformation;
import org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.listener.JdbcEventListener;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * To wrap the PreparedStatement.
 *
 * @author Gannalyo
 * @since 20190129
 */
public class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {

    private final PreparedStatement preparedStatement;
    private final PreparedStatementInformation preparedStatementInformation;
    private final JdbcEventListener eventListener;

    public static PreparedStatement wrap(PreparedStatement preparedStatement, PreparedStatementInformation preparedStatementInformation, JdbcEventListener eventListener) {
        if (preparedStatement == null) {
            return null;
        }
        return new PreparedStatementWrapper(preparedStatement, preparedStatementInformation, eventListener);
    }

    protected PreparedStatementWrapper(PreparedStatement delegate, PreparedStatementInformation preparedStatementInformation, JdbcEventListener eventListener) {
        super(delegate, eventListener);
        this.preparedStatement = delegate;
        this.preparedStatementInformation = preparedStatementInformation;
        this.eventListener = eventListener;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
            eventListener.onBeforeExecuteQuery(preparedStatementInformation);
            return ResultSetWrapper.wrap(preparedStatement.executeQuery(), new ResultSetInformation(), eventListener);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterExecuteQuery(preparedStatementInformation, System.nanoTime() - start, e);
        }
    }

    @SuppressWarnings("unchecked")
	@Override
    public int executeUpdate() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        int rowCount = 0;
        Map<JdbcEventListener, Object> listenerParams = null;
        try {
            //(Map<JdbcEventListener, Object>) eventListener.onBeforeExecuteUpdateWithReturnValue(preparedStatement, preparedStatementInformation);
            listenerParams = null;
            // 函数onBeforeExecuteUpdateWithReturnValue有返回值，导致在CompoundJdbcEventListener.onBeforeExecuteUpdateWithReturnValue中调用时仅执行了手动补偿中的对应方法，自动补偿中的对应方法无法被执行，去掉返回值可正常执行
            eventListener.onBeforeExecuteUpdate(preparedStatement, preparedStatementInformation);

            rowCount = preparedStatement.executeUpdate();

            return rowCount;
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterExecuteUpdateWithParams(preparedStatement, preparedStatementInformation, System.nanoTime() - start, rowCount, e, listenerParams);
        }
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNull(parameterIndex, sqlType);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, null, e);
        }
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBoolean(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setByte(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setShort(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setInt(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setLong(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setFloat(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setDouble(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBigDecimal(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setString(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBytes(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setDate(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setTime(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setTimestamp(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setAsciiStream(parameterIndex, x, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setUnicodeStream(parameterIndex, x, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBinaryStream(parameterIndex, x, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void clearParameters() throws SQLException {
        preparedStatement.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setObject(parameterIndex, x, targetSqlType);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setObject(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        boolean result = false;
        Map<JdbcEventListener, Object> listenerParams = null;
        try {
            //(Map<JdbcEventListener, Object>) eventListener.onBeforeExecuteUpdateWithReturnValue(preparedStatement, preparedStatementInformation);
            listenerParams = null;
            // 函数onBeforeExecuteUpdateWithReturnValue有返回值，导致在CompoundJdbcEventListener.onBeforeExecuteUpdateWithReturnValue中调用时仅执行了手动补偿中的对应方法，自动补偿中的对应方法无法被执行，去掉返回值可正常执行
            eventListener.onBeforeExecuteUpdate(preparedStatement, preparedStatementInformation);

            result = preparedStatement.execute();

            return result;
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterExecuteUpdateWithParams(preparedStatement, preparedStatementInformation, System.nanoTime() - start, result ? 1 : 0, e, listenerParams);
        }
    }

    @Override
    public void addBatch() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
            eventListener.onBeforeAddBatch(preparedStatementInformation);
            preparedStatement.addBatch();
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterAddBatch(preparedStatementInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setCharacterStream(parameterIndex, reader, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, reader, e);
        }
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setRef(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBlob(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setClob(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setArray(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setDate(parameterIndex, x, cal);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setTime(parameterIndex, x, cal);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setTimestamp(parameterIndex, x, cal);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNull(parameterIndex, sqlType, typeName);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, null, e);
        }
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setURL(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setRowId(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNString(parameterIndex, value);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, value, e);
        }
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNCharacterStream(parameterIndex, value, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, value, e);
        }
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNClob(parameterIndex, value);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, value, e);
        }
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setClob(parameterIndex, reader, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, reader, e);
        }
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBlob(parameterIndex, inputStream, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, inputStream, e);
        }
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNClob(parameterIndex, reader, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, reader, e);
        }
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setSQLXML(parameterIndex, xmlObject);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, xmlObject, e);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setAsciiStream(parameterIndex, x, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBinaryStream(parameterIndex, x, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setCharacterStream(parameterIndex, reader, length);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, reader, e);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setAsciiStream(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBinaryStream(parameterIndex, x);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, x, e);
        }
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setCharacterStream(parameterIndex, reader);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, reader, e);
        }
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNCharacterStream(parameterIndex, value);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, value, e);
        }
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setClob(parameterIndex, reader);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, reader, e);
        }
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setBlob(parameterIndex, inputStream);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, inputStream, e);
        }
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        SQLException e = null;
        try {
            preparedStatement.setNClob(parameterIndex, reader);
        } catch (SQLException sqlException) {
            e = sqlException;
            throw e;
        } finally {
            eventListener.onAfterPreparedStatementSet(preparedStatementInformation, parameterIndex, reader, e);
        }
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return preparedStatement.getParameterMetaData();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return preparedStatement.getMetaData();
    }

}
