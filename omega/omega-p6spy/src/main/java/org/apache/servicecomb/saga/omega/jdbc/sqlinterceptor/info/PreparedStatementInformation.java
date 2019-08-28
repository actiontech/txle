/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info;

import java.util.HashMap;
import java.util.Map;

/**
 * An information bean for the PreparedStatement.
 *
 * @author Gannalyo
 * @since 20190129
 */
public class PreparedStatementInformation {
    private final String sql;
    private final Map<Integer, Value> parameterValues = new HashMap<>();

    public PreparedStatementInformation(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    public String getSqlWithValues() {
        final StringBuilder sb = new StringBuilder();
        final String statementQuery = sql;

        int currentParameter = 0;
        for (int pos = 0; pos < statementQuery.length(); pos++) {
            char character = statementQuery.charAt(pos);
            if (statementQuery.charAt(pos) == '?' && currentParameter <= parameterValues.size()) {
                // replace with parameter value
                Value value = parameterValues.get(currentParameter);
                sb.append(value != null ? value.toString() : new Value().toString());
                currentParameter++;
            } else {
                sb.append(character);
            }
        }

        return sb.toString();
    }

    public void setParameterValue(final int position, final Object value) {
        parameterValues.put(position - 1, new Value(value));
    }

    protected Map<Integer, Value> getParameterValues() {
        return parameterValues;
    }

}
