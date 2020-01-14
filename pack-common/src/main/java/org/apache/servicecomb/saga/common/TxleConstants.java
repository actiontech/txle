/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class TxleConstants {
    public static final String APP_NAME = "txle";
    public static final String LOG_PREFIX = "[txle info] ";
    public static final String LOG_DEBUG_PREFIX = "[txle debug] ";
    public static final String LOG_ERROR_PREFIX = "[txle error] ";
    public static final String ACTION_SQL = " /**txle_sql**/";
    // Usage in org.apache.servicecomb.saga.alpha.server.GrpcTxEventEndpointImpl.onTxEvent
    public static final String SPECIAL_KEY = "txle-special-key";
    public static final String STRING_SEPARATOR = "_@@_";

    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";
    public static final int YES = 1;
    public static final int NO = 0;

    public static final String AUTO_COMPENSABLE_METHOD = "public boolean org.apache.servicecomb.saga.omega.transaction.AutoCompensateService.executeAutoCompensateByLocalTxId(java.lang.String,java.lang.String)";

    public static final String CONSUL_LEADER_KEY = APP_NAME + "/service/leader";
    public static final String CONSUL_LEADER_KEY_VALUE = "leader election key for " + TxleConstants.APP_NAME + " service";

    public enum Operation {
        DONOTHING,
        SUCCESS,
        FAIL
    }

    private TxleConstants() {
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

    public static String constructConfigCacheKey(String instanceId, String category, int type) {
        return "config_cache_" + instanceId + "_" + category + "_" + type;
    }
}
