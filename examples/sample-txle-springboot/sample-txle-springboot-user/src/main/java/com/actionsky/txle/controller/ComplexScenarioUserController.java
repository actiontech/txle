/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.service.ComplexScenarioUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2020/2/11
 */
@RestController
public class ComplexScenarioUserController {

    @Autowired
    private ComplexScenarioUserService userService;

    @PostMapping("/complexDeductMoneyFromUser/{userId}/{balance}")
    public String complexDeductMoneyFromUser(@PathVariable long userId, @PathVariable double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".deductMoneyFromUser'. \t\tParameters[userId = " + userId + ", balance = " + balance + "]");
        return userService.complexUpdateUserAuto(userId, balance) + "";
    }

}
