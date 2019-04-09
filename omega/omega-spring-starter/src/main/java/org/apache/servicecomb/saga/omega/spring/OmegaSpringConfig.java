/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.spring;

import org.apache.servicecomb.saga.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.saga.omega.connector.grpc.LoadBalancedClusterMessageSender;
import org.apache.servicecomb.saga.omega.context.*;
import org.apache.servicecomb.saga.omega.format.KryoMessageFormat;
import org.apache.servicecomb.saga.omega.format.MessageFormat;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.ClientAccidentPlatformService;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.apache.servicecomb.saga.omega.transaction.monitor.CommonPrometheusMetrics;
import org.apache.servicecomb.saga.omega.transaction.monitor.CompensableSqlMetrics;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Configuration
class OmegaSpringConfig {

  @Value("${utx.prometheus.metrics.port:8098}")
  private String promMetricsPort;

  @Value("${utx.accident.platform.address.api:\"\"}")
  private String accidentPlatformAddress;

  @Value("${utx.accident.platform.retry.retries:3}")
  private int retries;

  @Value("${utx.accident.platform.retry.interval:1}")
  private int interval;

  @Bean(name = {"omegaUniqueIdGenerator"})
  IdGenerator<String> idGenerator() {
    return new UniqueIdGenerator();
  }

  @Bean
  OmegaContext omegaContext(@Qualifier("omegaUniqueIdGenerator") IdGenerator<String> idGenerator) {
    return new OmegaContext(idGenerator);
  }

  @Bean
  CompensationContext compensationContext(OmegaContext omegaContext) {
    return new CompensationContext(omegaContext);
  }

  @Bean
  ServiceConfig serviceConfig(@Value("${spring.application.name}") String serviceName) {
    return new ServiceConfig(serviceName);
  }

  @Bean
  CommonPrometheusMetrics commonPrometheusMetrics() {
    return new CommonPrometheusMetrics(promMetricsPort);
  }

  @Bean
  CompensableSqlMetrics compensableSqlMetrics() {
    return new CompensableSqlMetrics(promMetricsPort);
  }

  @Bean
  AutoCompensableSqlMetrics autoCompensableSqlMetrics() {
    return new AutoCompensableSqlMetrics(promMetricsPort);
  }

  @Bean
  ClientAccidentPlatformService clientAccidentPlatformService(RestTemplate restTemplate) {
    return new ClientAccidentPlatformService(accidentPlatformAddress, retries, interval, restTemplate);
  }

  @Bean
  MessageSender grpcMessageSender(
      @Value("${alpha.cluster.address:localhost:8080}") String[] addresses,
      @Value("${alpha.cluster.ssl.enable:false}") boolean enableSSL,
      @Value("${alpha.cluster.ssl.mutualAuth:false}") boolean mutualAuth,
      @Value("${alpha.cluster.ssl.cert:client.crt}") String cert,
      @Value("${alpha.cluster.ssl.key:client.pem}") String key,
      @Value("${alpha.cluster.ssl.certChain:ca.crt}") String certChain,
      @Value("${omega.connection.reconnectDelay:3000}") int reconnectDelay,
      ServiceConfig serviceConfig,
      @Lazy MessageHandler handler) {

    MessageFormat messageFormat = new KryoMessageFormat();
    AlphaClusterConfig clusterConfig = new AlphaClusterConfig(Arrays.asList(addresses),
        enableSSL, mutualAuth, cert, key, certChain);
    final MessageSender sender = new LoadBalancedClusterMessageSender(
        clusterConfig,
        messageFormat,
        messageFormat,
        serviceConfig,
        handler,
        reconnectDelay);

    sender.onConnected();
    
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        sender.onDisconnected();
        sender.close();
      }
    }));
    return sender;
  }

}
