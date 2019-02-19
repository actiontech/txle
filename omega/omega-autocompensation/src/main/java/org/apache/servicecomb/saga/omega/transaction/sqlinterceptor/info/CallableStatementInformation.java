package org.apache.servicecomb.saga.omega.transaction.sqlinterceptor.info;

import java.util.HashMap;
import java.util.Map;

/**
 * An information bean for the CallableStatement.
 *
 * @author Gannalyo
 * @date 20190129
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
        for (String name : namedParameterValues.keySet()) {
            appendParameter(parameters, name, namedParameterValues.get(name));
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
