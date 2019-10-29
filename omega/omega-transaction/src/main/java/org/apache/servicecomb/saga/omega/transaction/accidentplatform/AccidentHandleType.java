/*
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.accidentplatform;

/**
 * An enum for the type of accident handling.
 *
 * @author Gannalyo
 * @since 2019/06/14
 */
public enum AccidentHandleType {
    ROLLBACK_ERROR,
    SEND_MESSAGE_ERROR;

    public int toInteger() {
        switch (this) {
            case ROLLBACK_ERROR:
                return 1;
            case SEND_MESSAGE_ERROR:
                return 2;
            default:
                return 1;
        }
    }
}
