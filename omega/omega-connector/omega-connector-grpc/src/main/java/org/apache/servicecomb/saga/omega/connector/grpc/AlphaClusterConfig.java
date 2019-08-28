/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import java.util.List;

public class AlphaClusterConfig {
  private List<String> addresses;

  private boolean enableSSL;

  private boolean enableMutualAuth;

  private String cert;

  private String key;

  private String certChain;

  public AlphaClusterConfig(List<String> addresses,
      boolean enableSSL,
      boolean enableMutualAuth,
      String cert,
      String key,
      String certChain) {
    this.addresses = addresses;
    this.enableMutualAuth = enableMutualAuth;
    this.enableSSL = enableSSL;
    this.cert = cert;
    this.key = key;
    this.certChain = certChain;
  }

  public List<String> getAddresses() {
    return addresses;
  }

  public boolean isEnableSSL() {
    return enableSSL;
  }

  public boolean isEnableMutualAuth() {
    return enableMutualAuth;
  }

  public String getCert() {
    return cert;
  }

  public String getKey() {
    return key;
  }

  public String getCertChain() {
    return certChain;
  }
}
