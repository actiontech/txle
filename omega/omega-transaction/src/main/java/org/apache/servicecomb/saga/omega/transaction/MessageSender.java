/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandling;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcConfigAck;

import java.util.Set;

public interface MessageSender {
  void onConnected();

  void onDisconnected();

  void close();

  String target();

  AlphaResponse send(TxEvent event);

  Set<String> send(Set<String> localTxIdSet);

  String reportMessageToServer(KafkaMessage message);

  String reportAccidentToServer(AccidentHandling accidentHandling);

  GrpcConfigAck readConfigFromServer(int type, String category);
}
