package org.apache.servicecomb.saga.omega.transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache tool for auto-compensation.
 * It will relate auto-compensation SQL to localTxId.
 * 
 * @author Gannalyo
 * @since 2018-07-30
 */
public class AutoCompensableCache {
	
	// To save currentThreadId and [globalTxId, localTxId].
	private static final Map<Long, String[]> curThreadIdAndGlobalLocalTxIdMap = new ConcurrentHashMap<>();

	public static void putThreadGlobalLocalTxId(long threadId, String globalTxId, String localTxId) {
		curThreadIdAndGlobalLocalTxIdMap.put(threadId, new String[]{globalTxId, localTxId});
	}
	
	public static String getGlobalTxIdByCurThreadId() {
		String[] txIds = curThreadIdAndGlobalLocalTxIdMap.get(Thread.currentThread().getId());
		if (txIds != null) {
			return txIds[0];
		}
		return "";
	}
	
	public static String getLocalTxIdByCurThreadId() {
		String[] txIds = curThreadIdAndGlobalLocalTxIdMap.get(Thread.currentThread().getId());
		if (txIds != null) {
			return txIds[1];
		}
		return "";
	}
	
	public static void clearCache(String localTxId) {
		curThreadIdAndGlobalLocalTxIdMap.remove(Thread.currentThread().getId());
	}
	
}
