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

    public String toDescription() {
        switch (this) {
            case Paid:
                return "已支付";
            case Failed:
                return "支付失败";
            default:
                return "支付失败";
        }
    }

    public static TransferStatusEnum convertStatusFromValue(int status) {
        switch (status) {
            case 1:
                return Paid;
            case 2:
                return Failed;
            default:
                return Failed;
        }
    }
}
