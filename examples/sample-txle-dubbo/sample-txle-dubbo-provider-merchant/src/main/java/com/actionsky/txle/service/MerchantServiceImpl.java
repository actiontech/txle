/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.dao.MerchantDao;
import com.alibaba.dubbo.config.annotation.Service;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gannalyo
 * @since 2019/4/3
 */
@Service
@Transactional
public class MerchantServiceImpl implements MerchantService {

    @Autowired
    private MerchantDao merchantDao;

    @Override
    @Compensable(compensationMethod = "payMoneyToMerchantRollback")
    public int payMoneyToMerchant(long merchantid, double balance) {
        int result =  merchantDao.updateBalanceById(merchantid, balance);
        if (balance > 200) {
            throw new RuntimeException("The 'Merchant' Service threw a runtime exception in case of balance was more than 200.");
        }
        return result;
    }

    public int payMoneyToMerchantRollback(long merchantid, double balance) {
        return merchantDao.updateBalanceById(merchantid, -balance);
    }

    @Override
    @AutoCompensable
    public int payMoneyToMerchantAuto(long merchantid, double balance) {
        return merchantDao.updateBalanceById(merchantid, balance);
    }
}
