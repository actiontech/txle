/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import java.io.File;
import java.util.Arrays;
import javax.net.ssl.SSLException;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;

@RunWith(SpringRunner.class)
@ActiveProfiles("ssl")
@SpringBootTest(classes = {AlphaApplication.class, AlphaConfig.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8092",
        "alpha.event.pollingInterval=1"
    })
public class AlphaIntegrationWithSSLTest extends AlphaIntegrationTest {
  private static final int port = 8092;

  @BeforeClass
  public static void setupClientChannel() {
    clientChannel = NettyChannelBuilder.forAddress("localhost", port)
        .negotiationType(NegotiationType.TLS)
        .sslContext(getSslContext())
        .build();
  }

  private static SslContext getSslContext(){
    ClassLoader classLoader = AlphaIntegrationWithSSLTest.class.getClassLoader();
    SslContext sslContext = null;
    try {
      sslContext = GrpcSslContexts.forClient().sslProvider(SslProvider.OPENSSL)
          .protocols("TLSv1.2","TLSv1.1")
          .ciphers(Arrays.asList("ECDHE-RSA-AES128-GCM-SHA256",
              "ECDHE-RSA-AES256-GCM-SHA384",
              "ECDHE-ECDSA-AES128-SHA256"))
          .trustManager(new File(classLoader.getResource("ca.crt").getFile()))
          .keyManager(new File(classLoader.getResource("client.crt").getFile()),
              new File(classLoader.getResource("client.pem").getFile())).build();
    } catch (SSLException e) {
      e.printStackTrace();
    }
    return sslContext;
  }

}
