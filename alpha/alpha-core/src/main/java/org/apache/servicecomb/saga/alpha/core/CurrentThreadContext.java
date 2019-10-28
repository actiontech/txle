/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context just for current thread.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
public final class CurrentThreadContext {

    private static final Map<String, TxEvent> GLOBAL_TX_EVENT_CONTEXT = new ConcurrentHashMap<>(10);

    public static TxEvent get(String globalTxId) {
        return GLOBAL_TX_EVENT_CONTEXT.get(globalTxId);
    }

    public static void put(String globalTxId, TxEvent event) {
        GLOBAL_TX_EVENT_CONTEXT.putIfAbsent(globalTxId, event);
    }

    private CurrentThreadContext() {
    }

    // It'll be invoked when the event is over or aborted.
    public static void clearCache(String globalTxId) {
        if (globalTxId != null) {
            GLOBAL_TX_EVENT_CONTEXT.remove(globalTxId);
        }
    }

}
