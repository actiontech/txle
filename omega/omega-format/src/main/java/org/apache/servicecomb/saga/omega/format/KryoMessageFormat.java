/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.format;

import java.io.ByteArrayInputStream;

import org.apache.servicecomb.saga.omega.transaction.OmegaException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

public class KryoMessageFormat implements MessageFormat {

  private static final int DEFAULT_BUFFER_SIZE = 4096;

  private static final KryoFactory FACTORY = () -> new Kryo();

  private static final KryoPool POOL = new KryoPool.Builder(FACTORY).softReferences().build();

  @Override
  public byte[] serialize(Object[] objects) {
    Output output = new Output(DEFAULT_BUFFER_SIZE, -1);

    Kryo kryo = POOL.borrow();
    kryo.writeObjectOrNull(output, objects, Object[].class);
    POOL.release(kryo);

    return output.toBytes();
  }

  @Override
  public Object[] deserialize(byte[] message) {
    try {
      Input input = new Input(new ByteArrayInputStream(message));

      Kryo kryo = POOL.borrow();
      Object[] objects = kryo.readObjectOrNull(input, Object[].class);
      POOL.release(kryo);

      return objects;
    } catch (KryoException e) {
      throw new OmegaException("Unable to deserialize message", e);
    }
  }
}
