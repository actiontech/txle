/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import static org.apache.servicecomb.saga.alpha.core.TxEventMaker.someEvent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PushBackOmegaCallbackTest {
  private static final Runnable NO_OP_RUNNABLE = () -> {
  };

  private final OmegaCallback underlying = Mockito.mock(OmegaCallback.class);
  private final BlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>();
  private final PushBackOmegaCallback pushBack = new PushBackOmegaCallback(runnables, underlying);

  @Before
  public void setUp() throws Exception {
    runnables.offer(NO_OP_RUNNABLE);
  }

  @Test
  public void pushFailedCallbackToEndOfQueue() throws Exception {
    TxEvent event = someEvent();
    doThrow(AlphaException.class).doThrow(AlphaException.class).doNothing().when(underlying).compensate(event);

    pushBack.compensate(event);

    assertThat(runnables.size(), is(2));
    assertThat(runnables.poll(), is(NO_OP_RUNNABLE));

    // failed again and pushed back itself to queue
    runnables.poll().run();
    assertThat(runnables.size(), is(1));

    runnables.poll().run();

    verify(underlying, times(3)).compensate(event);
  }
}
