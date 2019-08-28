/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

public class UniqueIdGeneratorTest {

  private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  private Callable<String> task = new Callable<String>() {
    @Override
    public String call() throws Exception {
      return idGenerator.nextId();
    }
  };

  @Test
  public void nextIdIsUnique() throws InterruptedException {
    int nThreads = 10;
    List<Callable<String>> tasks = Collections.nCopies(nThreads, task);
    ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
    List<Future<String>> futures = executorService.invokeAll(tasks);

    Set<String> ids = new HashSet<>();
    for (Future<String> future: futures) {
      try {
        ids.add(future.get());
      } catch (ExecutionException e) {
        fail("unable to retrieve next id, " + e);
      }
    }
    assertThat(ids.size(), is(nThreads));
  }
}
