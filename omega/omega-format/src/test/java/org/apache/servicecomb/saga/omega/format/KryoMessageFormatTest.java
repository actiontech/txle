/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.format;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

public class KryoMessageFormatTest extends MessageFormatTestBase {

  @BeforeClass
  public static void setUp() {
    format = new KryoMessageFormat();
  }

  @Test
  public void serializeEmptyClassIntoBytes() {
    byte[] bytes = format.serialize(new Object[]{new EmptyClass()});

    Object[] message = format.deserialize(bytes);

    assertThat(message[0], instanceOf(EmptyClass.class));
  }
}
