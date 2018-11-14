package org.apache.servicecomb.saga.alpha.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context just for current thread.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
public class CurrentThreadContext {

    private static final Map<String, TxEvent> globalTxEventContext = new ConcurrentHashMap<>(10);

    public static TxEvent get(String globalTxId) {
        return globalTxEventContext.get(globalTxId);
    }

    public static void put(String globalTxId, TxEvent event) {
        globalTxEventContext.putIfAbsent(globalTxId, event);
    }

    // It'll be invoked when the event is over or aborted.
    public static void clearCache(String globalTxId) {
        if (globalTxId != null) {
            globalTxEventContext.remove(globalTxId);
        }
    }

}
