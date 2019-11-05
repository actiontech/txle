/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 全局事务复杂场景
 *
 * @author Gannalyo
 * @since 2019/3/29
 */
@PropertySource({"classpath:service.properties"})
@RestController
public class ComplexScenarioGlobalTxController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${com.actionsky.txle.service.transferservice:http://localhost:8001}")
    private String transferServiceUrl;

    @Value("${com.actionsky.txle.service.userservice:http://localhost:8002}")
    private String userServiceUrl;

    @Value("${com.actionsky.txle.service.merchantservice:http://localhost:8003}")
    private String merchantServiceUrl;

    // 测试场景三：手动补偿，重试
    @SagaStart(category = "txle-springboot-global")
    @GetMapping("/testGlobalTransactionRetry/{userId}/{amount}/{merchantid}")
    public String testGlobalTransactionRetry(@PathVariable int userId, @PathVariable double amount, @PathVariable int merchantid) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".testGlobalTransactionRetry'. \t\tParameters[userId = " + userId + ", amount = " + amount + ", merchantid = " + merchantid + "]");

        try {
            // 1.记录交易
            restTemplate.postForObject(transferServiceUrl + "/createTransfer/{userId}/{amount}/{merchantid}", null, String.class, userId, amount, merchantid);

            // 2.扣款
            restTemplate.postForObject(userServiceUrl + "/deductMoneyFromUser/{userId}/{balance}", null, String.class, userId, amount);

            // 3.汇款 - 重试
            restTemplate.postForObject(merchantServiceUrl + "/payMoneyToMerchantRetry/{merchantid}/{balance}", null, String.class, merchantid, amount);

            return TxleConstants.OK;
        } catch (Exception e) {
            // 异常不能被捕获处理掉，否则如果try中报错(如远程接口调不通)，全局事务将不会回滚
            throw e;
        }
    }

    // 测试场景四：手动补偿，超时
    @SagaStart(category = "txle-springboot-global")
    @GetMapping("/testGlobalTransactionTimeout/{userId}/{amount}/{merchantid}")
    public String testGlobalTransactionTimeout(@PathVariable int userId, @PathVariable double amount, @PathVariable int merchantid) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".testGlobalTransactionTimeout'. \t\tParameters[userId = " + userId + ", amount = " + amount + ", merchantid = " + merchantid + "]");

        try {
            // 1.记录交易
            restTemplate.postForObject(transferServiceUrl + "/createTransfer/{userId}/{amount}/{merchantid}", null, String.class, userId, amount, merchantid);

            // 2.扣款
            restTemplate.postForObject(userServiceUrl + "/deductMoneyFromUser/{userId}/{balance}", null, String.class, userId, amount);

            // 3.汇款 - 超时
            restTemplate.postForObject(merchantServiceUrl + "/payMoneyToMerchantTimeout/{merchantid}/{balance}", null, String.class, merchantid, amount);

            return TxleConstants.OK;
        } catch (Exception e) {
            // 异常不能被捕获处理掉，否则如果try中报错(如远程接口调不通)，全局事务将不会回滚
            throw e;
        }
    }

    // 接收差错消息
    @PostMapping(value = "/receiveFailedGlobalTxInfo", produces = "application/json;charset=UTF-8")
    public String receiveFailedGlobalTxInfo(@RequestBody JSONObject jsonParams) {
        System.err.println("I am an Accident Platform, I received info - [" + jsonParams.toString() + "].");
        return "ok";
    }

}
