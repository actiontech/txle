/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.dao.UserDao;
import com.actionsky.txle.entity.UserEntity;
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
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    @Compensable(compensationMethod = "deductMoneyFromUserRollback")
    public int deductMoneyFromUser(long userId, double balance) {
        return updateBalance(userId, balance);
    }

    public int deductMoneyFromUserRollback(long userId, double balance) {
        return userDao.updateBalanceByUserId(userId, -balance);
    }

    @Override
    @AutoCompensable
    public int deductMoneyFromUserAuto(long userId, double balance) {
        return updateBalance(userId, balance);
    }

    private int updateBalance(long userId, double balance) {
        UserEntity userEntity = userDao.findOne(userId);
        if (userEntity != null) {
            if (userEntity.getBalance() < balance) {
                throw new RuntimeException("Sorry, not sufficient balance under your account.");
            }
        }
        return userDao.updateBalanceByUserId(userId, balance);
    }
}
