/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.format;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MessageFormatTestBase {

  static MessageFormat format;

  @Test
  public void serializeObjectIntoBytes() throws Exception {
    byte[] bytes = format.serialize(new Object[]{"hello", "world"});

    Object[] message = format.deserialize(bytes);

    assertThat(asList(message).containsAll(asList("hello", "world")), is(true));
  }

  @Test
  public void serializeNullIntoBytes() throws Exception {
    byte[] bytes = format.serialize(null);

    Object[] message = format.deserialize(bytes);

    assertThat(message, is(nullValue()));
  }

  @Test
  public void blowsUpWhenObjectIsNotDeserializable() throws Exception {
    try {
      format.deserialize(new byte[0]);
      expectFailing(OmegaException.class);
    } catch (OmegaException e) {
      assertThat(e.getMessage(), startsWith("Unable to deserialize message"));
    }
  }

  static class EmptyClass {
  }
}
