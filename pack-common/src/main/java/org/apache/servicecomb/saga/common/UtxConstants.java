package org.apache.servicecomb.saga.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class UtxConstants {
	public static final String LOG_PREFIX = "[utx info] ";
	public static final String LOG_DEBUG_PREFIX = "[utx debug] ";
	public static final String LOG_ERROR_PREFIX = "[utx error] ";
	public static final String ACTION_SQL = " /**utx_sql**/";
	public static final String SPECIAL_KEY = "UTX-SPECIAL-KEY";// Usage in org.apache.servicecomb.saga.alpha.server.GrpcTxEventEndpointImpl.onTxEvent

	private UtxConstants() {
	}

	public static String logPrefixWithTime() {
		return logPrefixWithTime(LOG_PREFIX);
	}

	public static String logDebugPrefixWithTime() {
		return logPrefixWithTime(LOG_DEBUG_PREFIX);
	}

	public static String logErrorPrefixWithTime() {
		return logPrefixWithTime(LOG_ERROR_PREFIX);
	}

	private static String logPrefixWithTime(String type) {
		if (!LOG_DEBUG_PREFIX.equals(type) && !LOG_ERROR_PREFIX.equals(type)) {
			type = LOG_PREFIX;
		}
		type = type.substring(0, type.length() - 2) + " ";
		return type + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()) + "] ";
	}
}
