package com.actionsky.txle.controller;

import com.actionsky.txle.service.MerchantService;
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
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @PostMapping("/payMoneyToMerchant/{merchantid}/{balance}")
    public String payMoneyToMerchant(@PathVariable long merchantid, @PathVariable double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".payMoneyToMerchant'. \t\tParameters[merchantid = " + merchantid + ", balance = " + balance + "]");
        merchantService.updateBalanceById(merchantid, balance);
        return TxleConstants.OK;
    }

    @PostMapping("/payMoneyToMerchantAuto/{merchantid}/{balance}")
    public String payMoneyToMerchantAuto(@PathVariable long merchantid, @PathVariable double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".payMoneyToMerchantAuto'. \t\tParameters[merchantid = " + merchantid + ", balance = " + balance + "]");
        merchantService.updateBalanceByIdAuto(merchantid, balance);
        return TxleConstants.OK;
    }

    @PostMapping("/highPerformance")
    public String highPerformance() {
        merchantService.highPerformance();
        return TxleConstants.OK;
    }
}
