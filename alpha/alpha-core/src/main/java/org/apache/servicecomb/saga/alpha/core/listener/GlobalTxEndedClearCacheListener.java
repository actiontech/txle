/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.listener;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.Observable;
import java.util.Observer;

/**
 * @author Gannalyo
 * @since 2019-08-31
 */
public class GlobalTxEndedClearCacheListener implements Observer {
	private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private ITxleCache txleCache;

	@Override
	public void update(Observable arg0, Object arg1) {
		if (arg0 != null) {
			TxEvent event = ((GlobalTxListener) arg0).getEvent();
			log.info("The listener [{}] observes the new event [" + event.toString() + "].", this.getClass());
			if (EventType.SagaEndedEvent.name().equals(event.type())) {
				txleCache.removeDistributedTxSuspendStatusCache(event.globalTxId());
				txleCache.removeDistributedTxAbortStatusCache(event.globalTxId());
			}
		}
	}

}
