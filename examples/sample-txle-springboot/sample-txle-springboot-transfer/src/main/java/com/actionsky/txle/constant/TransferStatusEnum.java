/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.constant;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
public enum TransferStatusEnum {
    Paid,
    Failed;

    public int toInteger() {
        switch (this) {
            case Paid:
                return 1;
            case Failed:
                return 2;
            default:
                return 1;
        }
    }
}
