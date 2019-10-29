/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.service.GlobalTransactionService;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@PropertySource({"classpath:service.properties"})
@RestController
public class GlobalTransactionController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${com.actionsky.txle.service.transferservice:http://localhost:8001}")
    private String transferServiceUrl;

    @Value("${com.actionsky.txle.service.userservice:http://localhost:8002}")
    private String userServiceUrl;

    @Value("${com.actionsky.txle.service.merchantservice:http://localhost:8003}")
    private String merchantServiceUrl;

    @Autowired
    private GlobalTransactionService service;

    // 测试场景一：手动补偿，模拟简易支付功能(记录交易、扣款、汇款)
    @SagaStart(category = "txle-springboot-global")
    @GetMapping("/testGlobalTransaction/{userId}/{amount}/{merchantid}")
    public String testGlobalTransaction(@PathVariable int userId, @PathVariable double amount, @PathVariable int merchantid) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".testGlobalTransaction'. \t\tParameters[userId = " + userId + ", amount = " + amount + ", merchantid = " + merchantid + "]");

        try {
            // 1.记录交易
//            restTemplate.postForObject(transferServiceUrl + "/createTransfer/{userId}/{amount}/{merchantid}", null, String.class, userId, amount, merchantid);

            // 2.扣款
            restTemplate.postForObject(userServiceUrl + "/deductMoneyFromUser/{userId}/{balance}", null, String.class, userId, amount);

            // 3.汇款
            restTemplate.postForObject(merchantServiceUrl + "/payMoneyToMerchant/{merchantid}/{balance}", null, String.class, merchantid, amount);

            return TxleConstants.OK;
        } catch (Exception e) {
            // 异常不能被捕获处理掉，否则如果try中报错(如远程接口调不通)，全局事务将不会回滚
            throw e;
        }
    }

    // 测试场景一：自动补偿，模拟简易支付功能(记录交易、扣款、汇款)
    @SagaStart(category = "txle-springboot-global")
    @GetMapping("/testGlobalTransactionAuto/{userId}/{amount}/{merchantid}")
    public String testGlobalTransactionAuto(@PathVariable int userId, @PathVariable double amount, @PathVariable int merchantid) {
        try {
            // 1.记录交易
//            restTemplate.postForObject(transferServiceUrl + "/createTransferAuto/{userId}/{amount}/{merchantid}", null, String.class, userId, amount, merchantid);
            // 2.扣款
            restTemplate.postForObject(userServiceUrl + "/deductMoneyFromUserAuto/{userId}/{balance}", null, String.class, userId, amount);
            // 3.汇款
            restTemplate.postForObject(merchantServiceUrl + "/payMoneyToMerchantAuto/{merchantid}/{balance}", null, String.class, merchantid, amount);
            return TxleConstants.OK;
        } catch (Exception e) {
            // 异常不能被捕获处理掉，否则如果try中报错(如远程接口调不通)，全局事务将不会回滚
            throw e;
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
