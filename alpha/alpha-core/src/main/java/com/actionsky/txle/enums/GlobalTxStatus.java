/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.enums;

public enum GlobalTxStatus {
    Running,
    Paused,
    Aborted,
    Over,
    Terminated,
    Degraded,
    FaultTolerant;

    public int toInteger() {
        switch (this) {
            case Running:
                return 0;
            case Paused:
                return 1;
            case Aborted:
                return 2;
            case Over:
                return 3;
            case Terminated:
                return 4;
            case Degraded:
                return 5;
            case FaultTolerant:
                return 6;
            default:
                return 0;
        }
    }

    public String toString() {
        switch (this) {
            case Running:
                return "running";
            case Paused:
                return "paused";
            case Aborted:
                return "aborted";
            case Over:
                return "over";
            case Terminated:
                return "terminated";
            case Degraded:
                return "degraded";
            case FaultTolerant:
                return "faulttolerant";
            default:
                return "running";
        }
    }

    public static GlobalTxStatus convertStatusFromValue(int status) {
        switch (status) {
            case 0:
                return Running;
            case 1:
                return Paused;
            case 2:
                return Aborted;
            case 3:
                return Over;
            case 4:
                return Terminated;
            case 5:
                return Degraded;
            case 6:
                return FaultTolerant;
            default:
                return Running;
        }
    }

}
