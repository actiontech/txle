package com.actionsky.txle.controller;

import com.actionsky.txle.service.ComplexScenarioMerchantService;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@RestController
public class ComplexScenarioMerchantController {

    @Autowired
    private ComplexScenarioMerchantService complexScenarioMerchantService;

    @PostMapping("/payMoneyToMerchantRetry/{merchantid}/{balance}")
    public String payMoneyToMerchantRetry(@PathVariable long merchantid, @PathVariable double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".payMoneyToMerchantRetry'. \t\tParameters[merchantid = " + merchantid + ", balance = " + balance + "]");
        complexScenarioMerchantService.updateBalanceByIdRetry(merchantid, balance);
        return TxleConstants.OK;
    }

    @PostMapping("/payMoneyToMerchantTimeout/{merchantid}/{balance}")
    public String payMoneyToMerchantTimeout(@PathVariable long merchantid, @PathVariable double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".payMoneyToMerchantTimeout'. \t\tParameters[merchantid = " + merchantid + ", balance = " + balance + "]");
        complexScenarioMerchantService.updateBalanceByIdTimeout(merchantid, balance);
        return TxleConstants.OK;
    }

}
