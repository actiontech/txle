package org.apache.servicecomb.saga.omega.jdbc.sqlinterceptor.info;

import java.text.SimpleDateFormat;

/**
 * To convert value type from Object to String.
 *
 * @author Gannalyo
 * @since 20190129
 */
public class Value {

    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private Object value;

    public Value(Object valueToSet) {
        this();
        this.value = valueToSet;
    }

    public Value() {
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return convertToString(this.value);
    }

    public String convertToString(Object value) {
        String result;
        if (value instanceof java.util.Date) {
            try {
                result = new SimpleDateFormat("dd-MMM-yy HH:mm:ss").format(value);
            } catch (Exception e) {
                try {
                    result = new SimpleDateFormat("dd-MMM-yy").format(value);
                } catch (Exception e1) {
                    throw e1;
                }
            }
        } else if (value instanceof Boolean) {
            result = value.toString();
        } else if (value instanceof byte[]) {
            result = toHexString((byte[]) value);
        } else {
            result = value.toString();
        }

        result = quoteIfNeeded(result, value);

        return result;
    }

    /**
     * @param bytes the bytes value to convert to {@link String}
     * @return the hexadecimal {@link String} representation of the given
     * {@code bytes}.
     */
    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int temp = (int) b & 0xFF;
            sb.append(HEX_CHARS[temp / 16]);
            sb.append(HEX_CHARS[temp % 16]);
        }
        return sb.toString();
    }

    private String quoteIfNeeded(String stringValue, Object obj) {
        if (stringValue == null) {
            return null;
        }

        if (Number.class.isAssignableFrom(obj.getClass()) || Boolean.class.isAssignableFrom(obj.getClass())) {
            return stringValue;
        } else {
            return "'" + escape(stringValue) + "'";
        }
    }

    private String escape(String stringValue) {
        return stringValue.replaceAll("'", "''");
    }

}
