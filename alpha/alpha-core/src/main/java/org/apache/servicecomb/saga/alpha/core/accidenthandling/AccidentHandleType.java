/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.accidenthandling;

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

    public static AccidentHandleType convertTypeFromValue(int type) {
        switch (type) {
            case 1:
                return ROLLBACK_ERROR;
            case 2:
                return SEND_MESSAGE_ERROR;
            default:
                return ROLLBACK_ERROR;
        }
    }

}
