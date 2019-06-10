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

package org.apache.servicecomb.saga.alpha.server;

import brave.Tracing;
import org.apache.servicecomb.saga.alpha.core.*;
import org.apache.servicecomb.saga.alpha.core.configcenter.DegradationConfigAspect;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.server.accidentplatform.ServerAccidentPlatformService;
import org.apache.servicecomb.saga.alpha.server.configcenter.ConfigCenterEntityRepository;
import org.apache.servicecomb.saga.alpha.server.configcenter.DBDegradationConfigService;
import org.apache.servicecomb.saga.alpha.server.kafka.KafkaProducerConfig;
import org.apache.servicecomb.saga.alpha.server.restapi.TransactionRestApi;
import org.apache.servicecomb.saga.alpha.server.tracing.TracingConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;

@EntityScan(basePackages = "org.apache.servicecomb.saga.alpha")
@Import({KafkaProducerConfig.class, TracingConfiguration.class})
@Configuration
class AlphaConfig {
  private final BlockingQueue<Runnable> pendingCompensations = new LinkedBlockingQueue<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Value("${alpha.compensation.retry.delay:3000}")
  private int delay;

  @Value("${utx.prometheus.metrics.port:-1}")
  private String promMetricsPort;

  @Value("${utx.accident.platform.address.api:\"\"}")
  private String accidentPlatformAddress;

  @Value("${utx.accident.platform.retry.retries:3}")
  private int retries;

  @Value("${utx.accident.platform.retry.interval:1}")
  private int interval;

  @Bean
  Map<String, Map<String, OmegaCallback>> omegaCallbacks() {
    return new ConcurrentHashMap<>();
  }

  @Bean
  OmegaCallback omegaCallback(Map<String, Map<String, OmegaCallback>> callbacks) {
    return new PushBackOmegaCallback(pendingCompensations, new CompositeOmegaCallback(callbacks));
  }
  
  @Bean
  TxEventRepository springTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    return new SpringTxEventRepository(eventRepo);
  }

  @Bean
  CommandRepository springCommandRepository(TxEventEnvelopeRepository eventRepo, CommandEntityRepository commandRepository) {
    return new SpringCommandRepository(eventRepo, commandRepository);
  }

  @Bean
  TxTimeoutRepository springTxTimeoutRepository(TxTimeoutEntityRepository timeoutRepo) {
    return new SpringTxTimeoutRepository(timeoutRepo);
  }

  @Bean
  ScheduledExecutorService compensationScheduler() {
    return scheduler;
  }

  @Bean
  GrpcServerConfig grpcServerConfig() { return new GrpcServerConfig(); }

  @Bean
  IConfigCenterService dbDegradationConfigService(ConfigCenterEntityRepository configCenterEntityRepository) { return new DBDegradationConfigService(configCenterEntityRepository); }

  @Bean
  UtxJpaRepositoryInterceptor utxJpaRepositoryInterceptor() {
    return new UtxJpaRepositoryInterceptor();
  }

  @Bean
  ServerAccidentPlatformService serverAccidentPlatformService(RestTemplate restTemplate) {
    return new ServerAccidentPlatformService(accidentPlatformAddress, retries, interval, restTemplate);
  }

  @Bean
  TxConsistentService txConsistentService(
      @Value("${alpha.event.pollingInterval:500}") int eventPollingInterval,
      GrpcServerConfig serverConfig,
      ScheduledExecutorService scheduler,
      TxEventRepository eventRepository,
      CommandRepository commandRepository,
      TxTimeoutRepository timeoutRepository,
      OmegaCallback omegaCallback,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks,
      IKafkaMessageProducer kafkaMessageProducer,
      IConfigCenterService dbDegradationConfigService,
      UtxMetrics utxMetrics,
      Tracing tracing) {

    new EventScanner(scheduler,
        eventRepository, commandRepository, timeoutRepository,
        omegaCallback, kafkaMessageProducer, utxMetrics, eventPollingInterval).run();

    TxConsistentService consistentService = new TxConsistentService(eventRepository, commandRepository, timeoutRepository);

    ServerStartable startable = buildGrpc(serverConfig, consistentService, omegaCallbacks, dbDegradationConfigService, tracing);
    new Thread(startable::start).start();

    return consistentService;
  }

  private ServerStartable buildGrpc(GrpcServerConfig serverConfig, TxConsistentService txConsistentService,
                                    Map<String, Map<String, OmegaCallback>> omegaCallbacks, IConfigCenterService dbDegradationConfigService, Tracing tracing) {
    return new GrpcStartable(serverConfig, tracing,
        new GrpcTxEventEndpointImpl(txConsistentService, omegaCallbacks, dbDegradationConfigService));
  }
  
  public TransactionRestApi transactionRestApi(TxConsistentService txConsistentService) {
	  return new TransactionRestApi(txConsistentService);
  }

  @Bean
  public UtxMetrics utxMetrics() {
    return new UtxMetrics(promMetricsPort);
  }

  @Bean
  public DegradationConfigAspect degradationConfigAspect() {
    return new DegradationConfigAspect();
  }

  @PostConstruct
  void init() {
    new PendingTaskRunner(pendingCompensations, delay).run();
  }

  @PreDestroy
  void shutdown() {
    scheduler.shutdownNow();
  }
}
