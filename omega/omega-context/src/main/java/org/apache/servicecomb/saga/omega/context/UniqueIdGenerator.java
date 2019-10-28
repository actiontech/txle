/*
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

import java.util.UUID;

public class UniqueIdGenerator implements IdGenerator<String> {
  @Override
  public String nextId() {
    return UUID.randomUUID().toString();
  }
}
