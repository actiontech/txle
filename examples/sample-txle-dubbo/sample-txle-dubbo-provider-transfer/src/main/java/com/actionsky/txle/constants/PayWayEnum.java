/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.constants;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
public enum PayWayEnum {
    Alipay,
    WeChat,
    Bank,
    Cash,
    Cheque,
    IOU;

    public int toInteger() {
        switch (this) {
            case Alipay:
                return 1;
            case WeChat:
                return 2;
            case Bank:
                return 3;
            case Cash:
                return 4;
            case Cheque:
                return 5;
            case IOU:
                return 6;
            default:
                return 1;
        }
    }

    public String toDescription() {
        switch (this) {
            case Alipay:
                return "支付宝";
            case WeChat:
                return "微信";
            case Bank:
                return "银行卡";
            case Cash:
                return "现金";
            case Cheque:
                return "支票";
            case IOU:
                return "白条";
            default:
                return "支付宝";
        }
    }

    public static PayWayEnum convertTypeFromValue(int type) {
        switch (type) {
            case 1:
                return Alipay;
            case 2:
                return WeChat;
            case 3:
                return Bank;
            case 4:
                return Cash;
            case 5:
                return Cheque;
            case 6:
                return IOU;
            default:
                return Alipay;
        }
    }
}
