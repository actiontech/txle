/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

public class PendingTaskRunnerTest {
  private final List<String> messages = new ArrayList<>();
  private final BlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>();
  private final PendingTaskRunner taskRunner = new PendingTaskRunner(runnables, 10);

  @Test
  public void burnsAllTasksInQueue() throws Exception {
    runnables.offer(() -> messages.add("hello"));
    runnables.offer(() -> messages.add("world"));

    taskRunner.run();

    await().atMost(500, MILLISECONDS).until(runnables::isEmpty);

    assertThat(messages, contains("hello", "world"));
  }

  @Test
  public void exitOnInterruption() throws Exception {
    taskRunner.run().cancel(true);

    runnables.offer(() -> messages.add("hello"));
    Thread.sleep(300);

    assertThat(runnables.isEmpty(), is(false));
    assertThat(messages.isEmpty(), is(true));
  }
}
