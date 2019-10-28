/*
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

/**
 * A cache tool for compensation.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
public final class CurrentThreadOmegaContext {

	private static final ThreadLocal<OmegaContextServiceConfig> CUR_THREAD_OMEGA_CONTEXT = new ThreadLocal<>();

	private CurrentThreadOmegaContext() {
	}

	public static void putThreadGlobalLocalTxId(OmegaContextServiceConfig context) {
		CUR_THREAD_OMEGA_CONTEXT.set(context);
	}

	public static OmegaContextServiceConfig getContextFromCurThread() {
		return CUR_THREAD_OMEGA_CONTEXT.get();
	}
	public static String getGlobalTxIdFromCurThread() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.globalTxId();
		}
		return "";
	}

	public static String getLocalTxIdFromCurThread() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.localTxId();
		}
		return "";
	}

	public static String getServiceNameFromCurThread() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.serviceName();
		}
		return "";
	}

	public static boolean isAutoCompensate() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.isAutoCompensate();
		}
		return false;
	}

	public static boolean isEnabledAutoCompensateTx() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.isEnabledAutoCompensateTx();
		}
		return false;
	}

	public static void clearCache() {
		CUR_THREAD_OMEGA_CONTEXT.remove();
	}

}
