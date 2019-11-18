/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package org.apache.servicecomb.saga.common;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author Gannalyo
 * @since 2019/11/18
 */
@Configuration
public class CommonConfig {

    @Value("${txle.compensable-retry:3}")
    private int times;

    @Value("${txle.compensable-retry.interval:3}")
    private int interval;

    @Bean
    public Retryer<Boolean> retryer() {
        return RetryerBuilder.<Boolean>newBuilder()
                .retryIfException()
                .retryIfResult(Predicates.equalTo(false))
                .withWaitStrategy(WaitStrategies.fixedWait(interval, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(times))
                .build();
    }

}
