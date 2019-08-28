/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import org.apache.servicecomb.saga.alpha.core.TxleJpaRepositoryProxyFactory;
import org.apache.servicecomb.saga.common.CommonConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnablePrometheusEndpoint
@SpringBootApplication
@EnableJpaRepositories(repositoryFactoryBeanClass = TxleJpaRepositoryProxyFactory.class)
@Import({CommonConfig.class})
public class AlphaApplication {
  public static void main(String[] args) {
    SpringApplication.run(AlphaApplication.class, args);
  }
}
