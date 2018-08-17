package com.p6spy.engine.autocompensate;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class AutoCompensableConstants {
	public static final String LOG_PREFIX = "[Action Saga] ";
	public static final String LOG_DEBUG_PREFIX = "[Action Saga Debug] ";
	public static final String LOG_ERROR_PREFIX = "[Action Saga Error] ";

	private AutoCompensableConstants() {
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
