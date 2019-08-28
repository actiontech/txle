/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import java.util.List;

public interface TxTimeoutRepository {
  void save(TxTimeout timeout);

  long findTxTimeoutByEventId(long eventId);

  void markTimeoutAsDone(List<Long> surrogateIdList);

  List<Long> selectTimeoutIdList();

  List<TxTimeout> findFirstTimeout();
}
