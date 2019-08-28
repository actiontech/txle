/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import java.util.List;

public interface CommandRepository {

  void saveCompensationCommands(String globalTxId);

  void saveCommandsForNeedCompensationEvent(String globalTxId, String localTxId);

  void saveWillCompensateCommandsForTimeout(String globalTxId);

  void saveWillCompensateCommandsForException(String globalTxId, String localTxId);

  void saveWillCompensateCommandsWhenGlobalTxAborted(String globalTxId);

  void saveWillCompensateCmdForCurSubTx(String globalTxId, String localTxId);

  void markCommandAsDone(String globalTxId, String localTxId);

  List<Command> findUncompletedCommands(String globalTxId);

  List<Command> findFirstCommandToCompensate();
}
