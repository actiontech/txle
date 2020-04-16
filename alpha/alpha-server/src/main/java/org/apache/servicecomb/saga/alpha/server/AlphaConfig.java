/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server;

import brave.Tracing;
import com.actionsky.txle.cache.ITxleEhCache;
import com.actionsky.txle.cache.TxleMysqlCache;
import com.actionsky.txle.configuration.TxleConfig;
import com.actionsky.txle.grpc.interfaces.CompensateService;
import com.actionsky.txle.grpc.interfaces.GlobalTxHandler;
import com.actionsky.txle.grpc.interfaces.GrpcTransactionEndpoint;
import com.actionsky.txle.grpc.interfaces.bizdbinfo.IBusinessDBLatestDetailService;
import org.apache.servicecomb.saga.alpha.core.*;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.alpha.core.configcenter.DegradationConfigAspect;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.datatransfer.IDataTransferService;
import org.apache.servicecomb.saga.alpha.core.listener.GlobalTxListener;
import org.apache.servicecomb.saga.alpha.core.listener.TxEventAfterPersistingListener;
import org.apache.servicecomb.saga.alpha.server.accidenthandling.AccidentHandlingEntityRepository;
import org.apache.servicecomb.saga.alpha.server.accidenthandling.AccidentHandlingService;
import org.apache.servicecomb.saga.alpha.server.configcenter.ConfigCenterEntityRepository;
import org.apache.servicecomb.saga.alpha.server.configcenter.DBDegradationConfigService;
import org.apache.servicecomb.saga.alpha.server.configcenter.ZkDegradationConfigService;
import org.apache.servicecomb.saga.alpha.server.datadictionary.DataDictionaryEntityRepository;
import org.apache.servicecomb.saga.alpha.server.datadictionary.DataDictionaryService;
import org.apache.servicecomb.saga.alpha.server.datatransfer.DataTransferRepository;
import org.apache.servicecomb.saga.alpha.server.datatransfer.DataTransferService;
import org.apache.servicecomb.saga.alpha.server.kafka.KafkaProducerConfig;
import org.apache.servicecomb.saga.alpha.server.tracing.TracingConfiguration;
import org.apache.servicecomb.saga.common.CommonConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;

@EntityScan(basePackages = {"org.apache.servicecomb.saga.alpha", "com.actionsky.txle"})
@EnableJpaRepositories(basePackages = {"org.apache.servicecomb.saga.alpha", "com.actionsky.txle"})
@Import({KafkaProducerConfig.class, TracingConfiguration.class, CommonConfig.class, TxleConfig.class})
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

  @Value("${alpha.event.pollingInterval:500}")
  private int eventPollingInterval;

  @Bean
  public RestTemplate restTemplate(@Qualifier("simpleClientHttpRequestFactory") ClientHttpRequestFactory clientHttpRequestFactory) {
    return new RestTemplate(clientHttpRequestFactory);
  }

  @Bean
  public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
    // setReadTimeout(5000); setConnectTimeout(15000);
    return new SimpleClientHttpRequestFactory();
  }

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
  DBDegradationConfigService dbDegradationConfigService(ConfigCenterEntityRepository configCenterEntityRepository) {
    return new DBDegradationConfigService(configCenterEntityRepository);
  }

  @Bean
  ZkDegradationConfigService zkDegradationConfigService(ConfigCenterEntityRepository configCenterEntityRepository) {
    return new ZkDegradationConfigService(configCenterEntityRepository);
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
  GlobalTxListener globalTxListener() {
    return new GlobalTxListener();
  }

  @Bean
  TxEventAfterPersistingListener txEventAfterPersistingListener(GlobalTxListener globalTxListener) {
    TxEventAfterPersistingListener txEventAfterPersistingListener = new TxEventAfterPersistingListener();
    globalTxListener.addObserver(txEventAfterPersistingListener);
    return txEventAfterPersistingListener;
  }

  @Bean
  TxConsistentService txConsistentService(TxEventRepository eventRepository, CommandRepository commandRepository, TxTimeoutRepository timeoutRepository) {
    return new TxConsistentService(eventRepository, commandRepository, timeoutRepository);
  }

  @Bean
  EventScanner eventScanner(TxEventRepository eventRepository, CommandRepository commandRepository, TxTimeoutRepository timeoutRepository,
                            OmegaCallback omegaCallback, TxleConsulClient txleConsulClient, TxleMysqlCache mysqlCache) {
    EventScanner eventScanner = new EventScanner(scheduler, eventRepository, commandRepository,
            timeoutRepository, omegaCallback, eventPollingInterval, txleConsulClient, mysqlCache);
    eventScanner.run();
    return eventScanner;
  }

  @Bean
  public ServerStartable serverStartable(TxConsistentService txConsistentService, GrpcServerConfig serverConfig,
                                         Map<String, Map<String, OmegaCallback>> omegaCallbacks,
                                         Tracing tracing, IAccidentHandlingService accidentHandlingService,
                                         GlobalTxHandler globalTxHandler, CompensateService compensateService, ITxleEhCache txleEhCache, TxleMysqlCache mysqlCache,
                                         TxEventRepository eventRepository, IBusinessDBLatestDetailService businessDBLatestDetailService) {
    ServerStartable starTable = buildGrpc(serverConfig, txConsistentService, omegaCallbacks, tracing, accidentHandlingService, globalTxHandler,
            compensateService, txleEhCache, mysqlCache, eventRepository, businessDBLatestDetailService);
    new Thread(starTable::start).start();
    return starTable;
  }

  private ServerStartable buildGrpc(GrpcServerConfig serverConfig, TxConsistentService txConsistentService, Map<String, Map<String, OmegaCallback>> omegaCallbacks,
                                    Tracing tracing, IAccidentHandlingService accidentHandlingService, GlobalTxHandler globalTxHandler, CompensateService compensateService,
                                    ITxleEhCache txleEhCache, TxleMysqlCache mysqlCache, TxEventRepository eventRepository, IBusinessDBLatestDetailService businessDBLatestDetailService) {
    return new GrpcStartable(serverConfig, tracing,
            new GrpcTxEventEndpointImpl(txConsistentService, omegaCallbacks, mysqlCache, accidentHandlingService),
            new GrpcTransactionEndpoint(globalTxHandler, compensateService, txleEhCache, mysqlCache, accidentHandlingService, eventRepository, txConsistentService, businessDBLatestDetailService));
  }

  @Bean
  public TxleMetrics txleMetrics() {
    return new TxleMetrics(promMetricsPort);
  }

  @Bean
  public DegradationConfigAspect degradationConfigAspect() {
    return new DegradationConfigAspect();
  }

  @Bean
  public TxleConsulClient txleConsulClient() {
    return new TxleConsulClient();
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
