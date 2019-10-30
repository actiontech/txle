/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.constant.PayWayEnum;
import com.actionsky.txle.constant.TransferStatusEnum;
import com.actionsky.txle.entity.TransferEntity;
import com.actionsky.txle.service.TransferService;
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
public class TransferController {

    @Autowired
    private TransferService transferService;

    @GetMapping("/createTransfer/{userId}/{merchantid}/{amount}/{payway}/{createtime}")
    public String createTransfer(@PathVariable("userId") long userId, @PathVariable("merchantid") long merchantid, @PathVariable("amount") double amount, @PathVariable("payway") int payway, @PathVariable("createtime") Date createtime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".createTransfer'. \t\tParameters[userId = " + userId + ", merchantid = " + merchantid + ", amount = " + amount + ", payway = " + payway + ", createtime = " + createtime + "]");

            return transferService.createTransfer(new TransferEntity(userId, merchantid, amount, PayWayEnum.convertTypeFromValue(payway), TransferStatusEnum.Paid, createtime));
        } catch (Exception e) {
            e.printStackTrace();
            return transferService.createTransfer(new TransferEntity(userId, merchantid, amount, PayWayEnum.convertTypeFromValue(payway), TransferStatusEnum.Failed, createtime));
        }
    }

    @GetMapping("/createTransferAuto/{userId}/{merchantid}/{amount}/{payway}")
    public String createTransferAuto(@PathVariable("userId") long userId, @PathVariable("merchantid") long merchantid, @PathVariable("amount") double amount, @PathVariable("payway") int payway) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".createTransferAuto'. \t\tParameters[userId = " + userId + ", merchantid = " + merchantid + ", amount = " + amount + ", payway = " + payway + "]");

            return transferService.createTransferAuto(new TransferEntity(userId, merchantid, amount, PayWayEnum.convertTypeFromValue(payway), TransferStatusEnum.Paid));
        } catch (Exception e) {
            e.printStackTrace();
            return transferService.createTransferAuto(new TransferEntity(userId, merchantid, amount, PayWayEnum.convertTypeFromValue(payway), TransferStatusEnum.Failed));
        }
    }
}
