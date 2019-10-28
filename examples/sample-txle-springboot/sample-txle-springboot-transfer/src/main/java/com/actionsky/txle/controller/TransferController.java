/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.controller;

import com.actionsky.txle.constant.PayWayEnum;
import com.actionsky.txle.constant.TransferStatusEnum;
import com.actionsky.txle.entity.TransferEntity;
import com.actionsky.txle.service.TransferService;
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
public class TransferController {

    @Autowired
    private TransferService transferService;

    @PostMapping("/createTransfer/{userId}/{amount}/{merchantid}")
    public String createTransfer(@PathVariable("userId") long userId, @PathVariable("amount") double amount, @PathVariable("merchantid") long merchantid) {
        // 强制去除毫秒，因为mysql5.6版本后会针对毫秒数进行四舍五入，而java全舍，导致毫秒数大于500时的时间相差1秒
        SimpleDateFormat sdfWithoutSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date createtime = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".createTransfer'. \t\tParameters[userId = " + userId + ", amount = " + amount + ", merchantid = " + merchantid + "]");

            createtime = sdfWithoutSecond.parse(sdfWithoutSecond.format(new Date()));
            transferService.createTransfer(new TransferEntity(userId, merchantid, amount, PayWayEnum.Alipay, TransferStatusEnum.Paid, createtime));
        } catch (Exception e) {
            transferService.createTransfer(new TransferEntity(userId, merchantid, amount, PayWayEnum.Alipay, TransferStatusEnum.Failed, createtime));
            e.printStackTrace();
        }
        return TxleConstants.OK;
    }

    @PostMapping("/createTransferAuto/{userId}/{amount}/{merchantid}")
    public String createTransferAuto(@PathVariable("userId") long userId, @PathVariable("amount") double amount, @PathVariable("merchantid") long merchantid) {
        // 强制去除毫秒，因为mysql5.6版本后会针对毫秒数进行四舍五入，而java全舍，导致毫秒数大于500时的时间相差1秒
        SimpleDateFormat sdfWithoutSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date createtime = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".createTransferAuto'. \t\tParameters[userId = " + userId + ", amount = " + amount + ", merchantid = " + merchantid + "]");

            createtime = sdfWithoutSecond.parse(sdfWithoutSecond.format(new Date()));
            transferService.createTransferAuto(new TransferEntity(userId, merchantid, amount, PayWayEnum.Alipay, TransferStatusEnum.Paid, createtime));
        } catch (Exception e) {
            transferService.createTransferAuto(new TransferEntity(userId, merchantid, amount, PayWayEnum.Alipay, TransferStatusEnum.Failed, createtime));
            e.printStackTrace();
        }
        return TxleConstants.OK;
    }
}
