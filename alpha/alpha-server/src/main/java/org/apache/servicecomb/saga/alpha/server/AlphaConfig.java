/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import brave.Tracing;
import org.apache.servicecomb.saga.alpha.core.*;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.alpha.core.configcenter.DegradationConfigAspect;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.datatransfer.IDataTransferService;
import org.apache.servicecomb.saga.alpha.core.listener.GlobalTxListener;
import org.apache.servicecomb.saga.alpha.core.listener.TxEventAfterPersistingListener;
import org.apache.servicecomb.saga.alpha.server.accidenthandling.AccidentHandlingEntityRepository;
import org.apache.servicecomb.saga.alpha.server.accidenthandling.AccidentHandlingService;
import org.apache.servicecomb.saga.alpha.server.cache.TxleCache;
import org.apache.servicecomb.saga.alpha.server.configcenter.ConfigCenterEntityRepository;
import org.apache.servicecomb.saga.alpha.server.configcenter.DBDegradationConfigService;
import org.apache.servicecomb.saga.alpha.server.datadictionary.DataDictionaryEntityRepository;
import org.apache.servicecomb.saga.alpha.server.datadictionary.DataDictionaryService;
import org.apache.servicecomb.saga.alpha.server.datatransfer.DataTransferRepository;
import org.apache.servicecomb.saga.alpha.server.datatransfer.DataTransferService;
import org.apache.servicecomb.saga.alpha.server.kafka.KafkaProducerConfig;
import org.apache.servicecomb.saga.alpha.server.tracing.TracingConfiguration;
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
  ITxleCache txleCache() {
    return new TxleCache();
  }

  @Bean
  StartingTask startingTask() {
    return new StartingTask();
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
  TxConsistentService txConsistentService(
          GrpcServerConfig serverConfig,
          ScheduledExecutorService scheduler,
          TxEventRepository eventRepository,
          CommandRepository commandRepository,
          TxTimeoutRepository timeoutRepository,
          OmegaCallback omegaCallback,
          Map<String, Map<String, OmegaCallback>> omegaCallbacks,
          IConfigCenterService dbDegradationConfigService,
          Tracing tracing,
          IAccidentHandlingService accidentHandlingService,
          ITxleCache txleCache,
          StartingTask startingTask) {

    new EventScanner(scheduler, eventRepository, commandRepository, timeoutRepository, omegaCallback, eventPollingInterval, txleCache, startingTask).run();

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
