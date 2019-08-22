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
import com.ecwid.consul.v1.ConsulClient;
import org.apache.servicecomb.saga.alpha.core.*;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.alpha.core.configcenter.DegradationConfigAspect;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.datatransfer.IDataTransferService;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.server.accidenthandling.AccidentHandlingEntityRepository;
import org.apache.servicecomb.saga.alpha.server.accidenthandling.AccidentHandlingService;
import org.apache.servicecomb.saga.alpha.server.configcenter.ConfigCenterEntityRepository;
import org.apache.servicecomb.saga.alpha.server.configcenter.DBDegradationConfigService;
import org.apache.servicecomb.saga.alpha.server.datadictionary.DataDictionaryEntityRepository;
import org.apache.servicecomb.saga.alpha.server.datadictionary.DataDictionaryService;
import org.apache.servicecomb.saga.alpha.server.datatransfer.DataTransferRepository;
import org.apache.servicecomb.saga.alpha.server.datatransfer.DataTransferService;
import org.apache.servicecomb.saga.alpha.server.kafka.KafkaProducerConfig;
import org.apache.servicecomb.saga.alpha.server.tracing.TracingConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;

@EntityScan(basePackages = "org.apache.servicecomb.saga.alpha")
@Import({KafkaProducerConfig.class, TracingConfiguration.class})
@EnableScheduling
@Configuration
class AlphaConfig {
  private final BlockingQueue<Runnable> pendingCompensations = new LinkedBlockingQueue<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Value("${alpha.compensation.retry.delay:3000}")
  private int delay;

  @Value("${txle.prometheus.metrics.port:-1}")
  private String promMetricsPort;

  @Value("${txle.accident.platform.address.api:\"\"}")
  private String accidentPlatformAddress;

  @Value("${txle.accident.platform.retry.retries:3}")
  private int retries;

  @Value("${txle.accident.platform.retry.interval:1}")
  private int interval;

  @Autowired(required = false)
  private ConsulClient consulClient;

  @Value("${spring.application.name:\"\"}")
  private String serverName;

  @Value("${spring.server.port:8090}")
  private int serverPort;

  @Value("${spring.cloud.consul.discovery.instanceId:\"\"}")
  private String consulInstanceId;

  @Value("${alpha.event.pollingInterval:500}")
  private int eventPollingInterval;

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
  GrpcServerConfig grpcServerConfig() {
    return new GrpcServerConfig();
  }

  @Bean
  IConfigCenterService dbDegradationConfigService(ConfigCenterEntityRepository configCenterEntityRepository) {
    return new DBDegradationConfigService(configCenterEntityRepository);
  }

  @Bean
  IDataDictionaryService dataDictionaryService(DataDictionaryEntityRepository dataDictionaryEntityRepository) {
    return new DataDictionaryService(dataDictionaryEntityRepository);
  }

  @Bean
  TxleJpaRepositoryInterceptor txleJpaRepositoryInterceptor() {
    return new TxleJpaRepositoryInterceptor();
  }

  @Bean
  IAccidentHandlingService accidentHandlingRepository(AccidentHandlingEntityRepository accidentHandlingEntityRepository, RestTemplate restTemplate) {
    return new AccidentHandlingService(accidentHandlingEntityRepository, accidentPlatformAddress, retries, interval, restTemplate);
  }

  @Bean
  IDataTransferService dataTransferService(DataTransferRepository dataTransferRepository, TxEventRepository txEventRepository) {
    return new DataTransferService(dataTransferRepository, txEventRepository);
  }

  @Bean
  TxConsistentService txConsistentService(
      GrpcServerConfig serverConfig,
      ScheduledExecutorService scheduler,
      TxEventRepository eventRepository,
      CommandRepository commandRepository,
      TxTimeoutRepository timeoutRepository,
      OmegaCallback omegaCallback,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks,
      IKafkaMessageProducer kafkaMessageProducer,
      IConfigCenterService dbDegradationConfigService,
      Tracing tracing,
      IAccidentHandlingService accidentHandlingService) {

    new EventScanner(scheduler,
        eventRepository, commandRepository, timeoutRepository,
        omegaCallback, kafkaMessageProducer, eventPollingInterval, consulClient, serverName, serverPort, consulInstanceId).run();

    TxConsistentService consistentService = new TxConsistentService(eventRepository, commandRepository, timeoutRepository);

    ServerStartable starTable = buildGrpc(serverConfig, consistentService, omegaCallbacks, dbDegradationConfigService, tracing, accidentHandlingService);
    new Thread(starTable::start).start();

    return consistentService;
  }

  private ServerStartable buildGrpc(GrpcServerConfig serverConfig, TxConsistentService txConsistentService,
                                    Map<String, Map<String, OmegaCallback>> omegaCallbacks, IConfigCenterService dbDegradationConfigService, Tracing tracing, IAccidentHandlingService accidentHandlingService) {
    return new GrpcStartable(serverConfig, tracing,
        new GrpcTxEventEndpointImpl(txConsistentService, omegaCallbacks, dbDegradationConfigService, accidentHandlingService));
  }

  @Bean
  public TxleMetrics txleMetrics() {
    return new TxleMetrics(promMetricsPort);
  }

  @Bean
  public DegradationConfigAspect degradationConfigAspect() {
    return new DegradationConfigAspect();
  }

  @PostConstruct
  void init() {
//    new PendingTaskRunner(pendingCompensations, delay).run();
  }

  @PreDestroy
  void shutdown() {
    scheduler.shutdownNow();
  }

}
