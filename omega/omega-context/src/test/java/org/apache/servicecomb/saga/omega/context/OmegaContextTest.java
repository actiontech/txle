/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.UUID;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;


public class OmegaContextTest {

  private final OmegaContext omegaContext = new OmegaContext(
      new IdGenerator() {
        @Override
        public Serializable nextId() {
          return "ignored";
        }
      });

  ExecutorService executor = Executors.newFixedThreadPool(2);



  @Test
  public void eachThreadGetsDifferentGlobalTxId() throws Exception {
    final CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable runnable = exceptionalRunnable(new ExceptionalRunnable() {

      @Override
      public void run() throws Exception {
        String txId = UUID.randomUUID().toString();
        omegaContext.setGlobalTxId(txId);
        barrier.await();

        assertThat(omegaContext.globalTxId(), is(txId));
      }
    });

    Future f1 = executor.submit(runnable);                                      ;
    Future f2 = executor.submit(runnable);
    f1.get();
    f2.get();

  }

  @Test
  public void eachThreadGetsDifferentLocalTxId() throws Exception {
    final CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable runnable = exceptionalRunnable(new ExceptionalRunnable() {

      @Override
      public void run() throws Exception {
        String spanId = UUID.randomUUID().toString();
        omegaContext.setLocalTxId(spanId);
        barrier.await();

        assertThat(omegaContext.localTxId(), is(spanId));
      }
    });

    Future f1 = executor.submit(runnable);                                      ;
    Future f2 = executor.submit(runnable);
    f1.get();
    f2.get();
  }

  private Runnable exceptionalRunnable(final ExceptionalRunnable runnable) {
    return new Runnable() {

      @Override
      public void run() {
        try {
          runnable.run();
        } catch (Exception e) {
          fail(e.getMessage());
        }
      }
    };
  }


  interface ExceptionalRunnable {
    void run() throws Exception;
  }
}
