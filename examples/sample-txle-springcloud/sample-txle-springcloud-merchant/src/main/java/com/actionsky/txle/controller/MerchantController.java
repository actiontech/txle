/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.service.MerchantService;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@RestController
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/payMoneyToMerchant/{merchantid}/{balance}")
    public String payMoneyToMerchant(@PathVariable("merchantid") long merchantid, @PathVariable("balance") double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".payMoneyToMerchant'. \t\tParameters[merchantid = " + merchantid + ", balance = " + balance + "]");
        merchantService.updateBalanceByMerchantId(merchantid, balance);
        return TxleConstants.OK;
    }

    @GetMapping("/payMoneyToMerchantAuto/{merchantid}/{balance}")
    public String payMoneyToMerchantAuto(@PathVariable("merchantid") long merchantid, @PathVariable("balance") double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".payMoneyToMerchantAuto'. \t\tParameters[merchantid = " + merchantid + ", balance = " + balance + "]");
        merchantService.updateBalanceByMerchantIdAuto(merchantid, balance);
        return TxleConstants.OK;
    }
}
