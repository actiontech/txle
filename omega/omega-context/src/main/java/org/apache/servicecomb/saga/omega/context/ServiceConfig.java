/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServiceConfig {
  private final String serviceName;
  private final String instanceId;

  public ServiceConfig(String serviceName) {
    this.serviceName = serviceName;
    try {
      instanceId = serviceName + "-" + InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  public String serviceName() {
    return serviceName;
  }

  public String instanceId() {
    return instanceId;
  }
}
