/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.accidenthandling;

/**
 * An enum for the status of accident handling.
 *
 * @author Gannalyo
 * @since 2019/06/14
 */
public enum AccidentHandleStatus {
    SENDING,
    SEND_OK,
    SEND_FAIL,
    HANDLE_OK,
    HANDLE_FAIL;

    public int toInteger() {
        switch (this) {
            case SENDING:
                return 0;
            case SEND_OK:
                return 1;
            case SEND_FAIL:
                return 2;
            case HANDLE_OK:
                return 3;
            case HANDLE_FAIL:
                return 4;
            default:
                return 0;
        }
    }

    public AccidentHandleStatus convertStatusFromValue(int status) {
        switch (status) {
            case 0:
                return SENDING;
            case 1:
                return SEND_OK;
            case 2:
                return SEND_FAIL;
            case 3:
                return HANDLE_OK;
            case 4:
                return HANDLE_FAIL;
            default:
                return SENDING;
        }
    }
}
