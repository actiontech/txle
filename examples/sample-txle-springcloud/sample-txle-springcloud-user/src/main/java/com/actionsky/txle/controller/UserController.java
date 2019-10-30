/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping("/deductMoneyFromUser/{userid}/{balance}")
    String deductMoneyFromUser(@PathVariable("userid") long userid, @PathVariable("balance") double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".testGlobalTransaction'. \t\tParameters[userid = " + userid + ", balance = " + balance + "]");

        System.err.println(request.getHeader(GLOBAL_TX_ID_KEY));

        return userService.updateBalanceByUserId(userid, balance);
    }

    @GetMapping("/deductMoneyFromUserAuto/{userid}/{balance}")
    String deductMoneyFromUserAuto(@PathVariable("userid") long userid, @PathVariable("balance") double balance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".testGlobalTransaction'. \t\tParameters[userid = " + userid + ", balance = " + balance + "]");

        return userService.updateBalanceByUserIdAuto(userid, balance);
    }

}
