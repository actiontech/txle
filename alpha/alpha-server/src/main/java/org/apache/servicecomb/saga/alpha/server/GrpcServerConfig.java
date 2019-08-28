/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class GrpcServerConfig {
  @Value("${alpha.server.host:0.0.0.0}")
  private String host;

  @Value("${alpha.server.port:8080}")
  private int port;

  @Value("${alpha.server.ssl.enable:false}")
  private boolean sslEnable;

  @Value("${alpha.server.ssl.cert:server.crt}")
  private String cert;

  @Value("${alpha.server.ssl.key:server.pem}")
  private String key;

  @Value("${alpha.server.ssl.mutualAuth:false}")
  private boolean mutualAuth;

  @Value("${alpha.server.ssl.clientCert:client.crt}")
  private String clientCert;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean isSslEnable() {
    return sslEnable;
  }

  public String getCert() {
    return cert;
  }

  public String getKey() {
    return key;
  }

  public boolean isMutualAuth() {
    return mutualAuth;
  }

  public String getClientCert() {
    return clientCert;
  }
}


