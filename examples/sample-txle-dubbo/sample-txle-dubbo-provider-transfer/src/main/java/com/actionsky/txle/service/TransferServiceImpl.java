/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.constants.TransferStatusEnum;
import com.actionsky.txle.dao.TransferDao;
import com.alibaba.dubbo.config.annotation.Service;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/4/3
 */
@Service
@Transactional
public class TransferServiceImpl implements TransferService {

    @Autowired
    private TransferDao transferDao;

    @Override
    @Compensable(compensationMethod = "createTransferRollback")
    public int createTransfer(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("createtime") Date createtime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".createTransfer'. \t\tParameters[userid = " + userid + ", merchantid = " + merchantid + ", amount = " + amount + ", payway = " + payway + ", createtime = " + createtime + "]");
        return transferDao.save(userid, merchantid, amount, payway, TransferStatusEnum.Paid.toInteger(), 1, createtime);
    }

    public boolean createTransferRollback(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("createtime") Date createtime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        System.err.println("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".createTransferRollback'. \t\tParameters[userid = " + userid + ", merchantid = " + merchantid + ", amount = " + amount + ", payway = " + payway + ", createtime = " + createtime + "]");
        return transferDao.updateTransferStatus(userid, merchantid, amount, payway, TransferStatusEnum.Paid.toInteger(), 1, createtime, TransferStatusEnum.Failed.toInteger()) > 0;
    }

    @Override
    @AutoCompensable
    public int createTransferAuto(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("createtime") Date createtime) {
        // 自动补偿回滚与手动补偿不一致，自动补偿针对insert语句会执行对应的delete语句，而手动补偿自己按照业务需求将交易记录状态改为了失败
        return transferDao.save(userid, merchantid, amount, payway, TransferStatusEnum.Paid.toInteger(), 1, createtime);
    }

}
