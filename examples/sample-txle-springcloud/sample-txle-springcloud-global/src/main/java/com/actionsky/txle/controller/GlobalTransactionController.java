/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.api.MerchantFeignClient;
import com.actionsky.txle.api.TransferFeignClient;
import com.actionsky.txle.api.UserFeignClient;
import com.actionsky.txle.service.GlobalTransactionService;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
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
public class GlobalTransactionController {

    @Autowired
    private TransferFeignClient transferFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private MerchantFeignClient merchantFeignClient;

    @Autowired
    private GlobalTransactionService service;

    // 测试场景一：手动补偿，模拟简易支付功能(记录交易、扣款、汇款)
    @SagaStart(category = "txle-springcloud-global")
    @GetMapping("/testGlobalTransaction/{userId}/{amount}/{merchantid}")
    public String testGlobalTransaction(@PathVariable int userId, @PathVariable double amount, @PathVariable int merchantid) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".testGlobalTransaction'. \t\tParameters[userId = " + userId + ", amount = " + amount + ", merchantid = " + merchantid + "]");

        try {
            // 1.记录交易
            // 强制去除毫秒，因为mysql5.6版本后会针对毫秒数进行四舍五入，而java全舍，导致毫秒数大于500时的时间相差1秒
            SimpleDateFormat sdfWithoutSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            transferFeignClient.createTransfer(userId, merchantid, amount, 1);

            // 2.扣款
            userFeignClient.deductMoneyFromUser(userId, amount);

            // 3.汇款
            merchantFeignClient.payMoneyToMerchant(userId, amount);

            return TxleConstants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    // 测试场景一：自动补偿，模拟简易支付功能(记录交易、扣款、汇款)
    @SagaStart(category = "txle-springcloud-global")
    @GetMapping("/testGlobalTransactionAuto/{userId}/{amount}/{merchantid}")
    public String testGlobalTransactionAuto(@PathVariable int userId, @PathVariable double amount, @PathVariable int merchantid) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".testGlobalTransactionAuto'. \t\tParameters[userId = " + userId + ", amount = " + amount + ", merchantid = " + merchantid + "]");

        try {
            // 1.记录交易
            // 强制去除毫秒，因为mysql5.6版本后会针对毫秒数进行四舍五入，而java全舍，导致毫秒数大于500时的时间相差1秒
            SimpleDateFormat sdfWithoutSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            transferFeignClient.createTransferAuto(userId, merchantid, amount, 1);

            // 2.扣款
            userFeignClient.deductMoneyFromUserAuto(userId, amount);

            // 3.汇款
            merchantFeignClient.payMoneyToMerchantAuto(userId, amount);

            return TxleConstants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    // 性能测试接口：集成txle
    @SagaStart(category = "highPerformance")
    @GetMapping("/highPerformance")
    public String highPerformance() {
        try {
            service.highPerformance();
            service.highPerformance();
        } catch (Exception e) {
            throw e;
        }
        return TxleConstants.OK;
    }

    // 性能测试接口：普通业务，未集成txle
    @GetMapping("/highPerformanceWithoutTxle")
    public String highPerformanceWithoutTxle() {
        try {
            service.highPerformanceWithoutTxle();
            service.highPerformanceWithoutTxle();
        } catch (Exception e) {
            throw e;
        }
        return TxleConstants.OK;
    }
}
