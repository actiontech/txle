/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.configcenter;

public enum ConfigCenterStatus {
    Normal,
    Historical,
    Dumped;

    public int toInteger() {
        switch (this) {
            case Normal:
                return 0;
            case Historical:
                return 1;
            case Dumped:
                return 2;
            default:
                return 0;
        }
    }

    public ConfigCenterStatus convertStatusFromValue(int status) {
        switch (status) {
            case 0:
                return Normal;
            case 1:
                return Historical;
            case 2:
                return Dumped;
            default:
                return Normal;
        }
    }
}
