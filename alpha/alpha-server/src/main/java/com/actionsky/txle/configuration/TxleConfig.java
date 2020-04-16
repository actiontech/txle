/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.configuration;

import com.actionsky.txle.cache.*;
import com.actionsky.txle.grpc.interfaces.CompensateService;
import com.actionsky.txle.grpc.interfaces.CustomRepository;
import com.actionsky.txle.grpc.interfaces.GlobalTxHandler;
import com.actionsky.txle.grpc.interfaces.ICustomRepository;
import com.actionsky.txle.grpc.interfaces.bizdbinfo.BusinessDBLatestDetailRepository;
import com.actionsky.txle.grpc.interfaces.bizdbinfo.BusinessDBLatestDetailService;
import com.actionsky.txle.grpc.interfaces.bizdbinfo.IBusinessDBLatestDetailService;
import com.actionsky.txle.grpc.interfaces.eventaddition.ITxEventAdditionService;
import com.actionsky.txle.grpc.interfaces.eventaddition.TxEventAdditionRepository;
import com.actionsky.txle.grpc.interfaces.eventaddition.TxEventAdditionService;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EntityScan(basePackages = {"com.actionsky.txle"})
@Import({EhCacheConfig.class})
@Configuration
public class TxleConfig {

    @Bean
    public ITxleEhCache txleEhCache() {
        return new TxleEhCache();
    }

    @Bean
    public TxleMysqlCache txleMysqlCache() {
        return new TxleMysqlCache();
    }

    @Bean
    public TxleRedisCache txleRedisCache() {
        return new TxleRedisCache();
    }

    @Bean
    public ICustomRepository customRepository() {
        return new CustomRepository();
    }

    @Bean
    public GlobalTxHandler globalTxHandler() {
        return new GlobalTxHandler();
    }

    @Bean
    public CompensateService compensateService() {
        return new CompensateService();
    }

    @Bean
    public ITxEventAdditionService eventAdditionService(TxEventAdditionRepository eventAdditionRepository) {
        return new TxEventAdditionService(eventAdditionRepository);
    }

    @Bean
    public IBusinessDBLatestDetailService businessDBLatestDetailService(BusinessDBLatestDetailRepository businessDBLatestDetailRepository) {
        return new BusinessDBLatestDetailService(businessDBLatestDetailRepository);
    }

}
