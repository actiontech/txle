/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PushBackReconnectRunnable implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final MessageSender messageSender;
  private final Map<MessageSender, Long> senders;
  private final BlockingQueue<Runnable> pendingTasks;

  private final BlockingQueue<MessageSender> connectedSenders;

  PushBackReconnectRunnable(
      MessageSender messageSender,
      Map<MessageSender, Long> senders,
      BlockingQueue<Runnable> pendingTasks,
      BlockingQueue<MessageSender> connectedSenders) {
    this.messageSender = messageSender;
    this.senders = senders;
    this.pendingTasks = pendingTasks;
    this.connectedSenders = connectedSenders;
  }

  @Override
  public void run() {
    try {
      LOG.info("Retry connecting to alpha at {}", messageSender.target());
      messageSender.onDisconnected();
      messageSender.onConnected();
      senders.put(messageSender, 0L);
      connectedSenders.offer(messageSender);
      LOG.info("Retry connecting to alpha at {} is successful", messageSender.target());
    } catch (Exception e) {
      LOG.error("Failed to reconnect to alpha at {}", messageSender.target(), e);
      pendingTasks.offer(this);
    }
  }
}
