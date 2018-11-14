package org.apache.servicecomb.saga.omega.context;

import org.apache.servicecomb.saga.omega.context.OmegaContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache tool for compensation.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
public class CurrentThreadOmegaContext {
	
	private static final ThreadLocal<OmegaContextServiceConfig> curThreadOmegaContext = new ThreadLocal<>();

	public static void putThreadGlobalLocalTxId(OmegaContextServiceConfig context) {
		curThreadOmegaContext.set(context);
	}
	
	public static OmegaContextServiceConfig getContextFromCurThread() {
		return curThreadOmegaContext.get();
	}
	public static String getGlobalTxIdFromCurThread() {
		OmegaContextServiceConfig context = curThreadOmegaContext.get();
		if (context != null) {
			return context.globalTxId();
		}
		return "";
	}

	public static String getLocalTxIdFromCurThread() {
		OmegaContextServiceConfig context = curThreadOmegaContext.get();
		if (context != null) {
			return context.localTxId();
		}
		return "";
	}

	public static String getServiceNameFromCurThread() {
		OmegaContextServiceConfig context = curThreadOmegaContext.get();
		if (context != null) {
			return context.serviceName();
		}
		return "";
	}

	public static void clearCache() {
		curThreadOmegaContext.remove();
	}
	
}
