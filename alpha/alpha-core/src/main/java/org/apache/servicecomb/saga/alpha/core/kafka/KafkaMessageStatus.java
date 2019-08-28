/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.kafka;

/**
 * An enum for the status of kafka message.
 *
 * @author Gannalyo
 * @since 2018/12/13
 */
public enum KafkaMessageStatus {
    INIT,
    SENDING,
    SUCCESSFUL,
    FAILED;

    public int toInteger() {
        switch (this) {
            case INIT:
                return 0;
            case SENDING:
                return 1;
            case SUCCESSFUL:
                return 2;
            case FAILED:
                return 3;
            default:
                return 0;
        }
    }

    public KafkaMessageStatus convertStatusFromValue(int status) {
        switch (status) {
            case 0:
                return INIT;
            case 1:
                return SENDING;
            case 2:
                return SUCCESSFUL;
            case 3:
                return FAILED;
            default:
                return INIT;
        }
    }
}
