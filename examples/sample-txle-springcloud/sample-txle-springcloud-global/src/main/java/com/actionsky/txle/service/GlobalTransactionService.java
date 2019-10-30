/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Service
public class GlobalTransactionService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Transactional
    @Compensable(compensationMethod = "highPerformanceRollback")
    public void highPerformance() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        LOG.error("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".highPerformance'.");
    }

    public void highPerformanceRollback() {
    }

    @Transactional
    public void highPerformanceWithoutTxle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        LOG.error("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".highPerformance'.");
    }
}
