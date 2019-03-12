package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An information bean for the ResultSetStatement.
 *
 * @author Gannalyo
 * @date 20190129
 */
public class ResultSetInformation {

    private final Map<String, Value> resultMap = new LinkedHashMap<String, Value>();

    public ResultSetInformation() {}

    public void setColumnValue(String columnName, Object value) {
        resultMap.put(columnName, new Value(value));
    }

    public String getSqlWithValues() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Value> entry : resultMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey());
            sb.append(" = ");
            sb.append(entry.getValue() != null ? entry.getValue().toString() : new Value().toString());
        }

        return sb.toString();
    }

}
