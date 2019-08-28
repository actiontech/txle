/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PendingTaskRunner {
  private final BlockingQueue<Runnable> pendingTasks;
  private final int delay;

  public PendingTaskRunner(BlockingQueue<Runnable> pendingTasks, int delay) {
    this.pendingTasks = pendingTasks;
    this.delay = delay;
  }

  public Future<?> run() {
    return Executors.newSingleThreadScheduledExecutor()
        .scheduleWithFixedDelay(() -> {
          try {
            pendingTasks.take().run();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }, 0, delay, MILLISECONDS);
  }
}
