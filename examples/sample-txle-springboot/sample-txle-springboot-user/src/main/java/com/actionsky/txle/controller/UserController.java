/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.service.UserService;
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
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/deductMoneyFromUser/{userId}/{balance}")
    public String deductMoneyFromUser(@PathVariable long userId, @PathVariable double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".deductMoneyFromUser'. \t\tParameters[userId = " + userId + ", balance = " + balance + "]");
        return userService.updateBalanceByUserId(userId, balance) + "";
    }

    @PostMapping("/deductMoneyFromUserAuto/{userId}/{balance}")
    public String deductMoneyFromUserAuto(@PathVariable long userId, @PathVariable double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".deductMoneyFromUserAuto'. \t\tParameters[userId = " + userId + ", balance = " + balance + "]");
        synchronized (userService) {
            // 防止并发场景同时更新数据库，避免负数情况
            return userService.updateBalanceByUserIdAuto(userId, balance) + "";
        }
    }

    @PostMapping("/highPerformance")
    public String highPerformance() {
        userService.highPerformance();
        return TxleConstants.OK;
    }

}
