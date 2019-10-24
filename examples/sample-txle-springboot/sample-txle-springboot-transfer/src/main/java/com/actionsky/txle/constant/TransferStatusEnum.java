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
