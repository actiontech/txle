/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.cache;

/**
 * @author Gannalyo
 * @since 2020/2/20
 */
public enum TxleCacheType {
    INIT,
    CONFIG,
    GLOBALTX,
    OTHER;

    public String toString() {
        switch (this) {
            case INIT:
                return "init";
            case CONFIG:
                return "config";
            case GLOBALTX:
                return "globalTx";
            case OTHER:
                return "other";
            default:
                return "default";
        }
    }

}
