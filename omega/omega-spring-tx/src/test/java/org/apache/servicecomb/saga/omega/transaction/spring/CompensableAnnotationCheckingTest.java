/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CompensableAnnotationCheckingTest {
  @Test
  public void blowsUpWhenCompensableMethodIsNotFound() throws Exception {
    try {
      try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(TransactionTestMain.class)
          .profiles("annotation-checking")
          .run()) {
        expectFailing(BeanCreationException.class);
      }
    } catch (BeanCreationException e) {
      assertThat(e.getCause().getMessage(), startsWith("No such compensation method [none]"));
    }
  }

  @Test
  public void blowsUpWhenAnnotationOnWrongType() throws Exception {
    try {
      try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(TransactionTestMain.class)
          .profiles("omega-context-aware-checking")
          .run()) {
        expectFailing(BeanCreationException.class);
      }
    } catch (BeanCreationException e) {
      assertThat(e.getCause().getMessage(),
          is("Only Executor, ExecutorService, and ScheduledExecutorService are supported for @OmegaContextAware"));
    }
  }
}
