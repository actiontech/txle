/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.TxTimeout;
import org.apache.servicecomb.saga.alpha.core.TxTimeoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.PENDING;

public class SpringTxTimeoutRepository implements TxTimeoutRepository {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxTimeoutEntityRepository timeoutRepo;

  SpringTxTimeoutRepository(TxTimeoutEntityRepository timeoutRepo) {
    this.timeoutRepo = timeoutRepo;
  }

  @Override
  public void save(TxTimeout timeout) {
    try {
      timeoutRepo.save(timeout);
    } catch (Exception ignored) {
      LOG.warn("Failed to save some timeout {}", timeout);
    }
  }

  @Override
  public long findTxTimeoutByEventId(long eventId) {
    return timeoutRepo.findTxTimeoutByEventId(eventId);
  }

  @Override
  public void markTimeoutAsDone(List<Long> surrogateIdList) {
    timeoutRepo.updateStatusOfFinishedTx(surrogateIdList);
  }

  @Override
  public List<Long> selectTimeoutIdList() {
    return timeoutRepo.selectTimeoutIdList();
  }

  @Transactional
  @Override
  public List<TxTimeout> findFirstTimeout() {
    List<TxTimeout> timeoutEvents = timeoutRepo.findFirstTimeoutTxOrderByExpireTimeAsc(new Date());
    timeoutEvents.forEach(event -> timeoutRepo
        .updateStatusByGlobalTxIdAndLocalTxId(PENDING.name(), event.globalTxId(), event.localTxId()));
    return timeoutEvents;
  }
}