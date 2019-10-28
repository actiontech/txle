/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An information bean for the CallableStatement.
 *
 * @author Gannalyo
 * @since 20190129
 */
public class CallableStatementInformation extends PreparedStatementInformation {
    private final Map<String, Value> namedParameterValues = new HashMap<String, Value>();

    public CallableStatementInformation(String sql) {
        super(sql);
    }

    @Override
    public String getSqlWithValues() {
        if (namedParameterValues.size() == 0) {
            return super.getSqlWithValues();
        }

        final StringBuilder result = new StringBuilder();
        final String statementQuery = super.getSql();

        // first append the original statement
        result.append(statementQuery);
        result.append(" ");

        StringBuilder parameters = new StringBuilder();

        // add parameters set with ordinal positions
        for (Integer position : getParameterValues().keySet()) {
            appendParameter(parameters, position.toString(), getParameterValues().get(position));
        }

        // add named parameters
        Iterator<Map.Entry<String, Value>> iterator = namedParameterValues.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Value> entry = iterator.next();
            appendParameter(parameters, entry.getKey(), entry.getValue());
        }

        result.append(parameters);

        return result.toString();
    }

    private void appendParameter(StringBuilder parameters, String name, Value value) {
        if (parameters.length() > 0) {
            parameters.append(", ");
        }

        parameters.append(name);
        parameters.append(":");
        parameters.append(value != null ? value.toString() : new Value().toString());
    }

    public void setParameterValue(final String name, final Object value) {
        namedParameterValues.put(name, new Value(value));
    }
}
