/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.listener;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.common.EventType;

import java.util.Observable;

/**
 * @author Gannalyo
 * @since 2019-08-31
 */
public class GlobalTxListener extends Observable {
    private TxEvent event;

    public TxEvent getEvent() {
        return event;
    }

    public void listenEvent(TxEvent event) {
        this.event = event;
        // a mark for changing to data
        setChanged();

        // to notify all observers when the data was changed
        notifyObservers();
    }
}
