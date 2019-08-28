/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

public class AlphaResponse {
    private final boolean aborted;
    // The paused status for global transaction By Gannalyo
    private final boolean paused;
    private final boolean enabledTx;

    public AlphaResponse(boolean aborted) {
        this.aborted = aborted;
        this.paused = false;
        this.enabledTx = true;
    }

    public AlphaResponse(boolean aborted, boolean paused, boolean enabledTx) {
        this.aborted = aborted;
        this.paused = paused;
        this.enabledTx = enabledTx;
    }

    public boolean aborted() {
        return aborted;
    }

    public boolean paused() {
        return paused;
    }

    public boolean enabledTx() {
        return enabledTx;
    }

}
