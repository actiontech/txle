/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.entity.UserEntity;
import com.actionsky.txle.repository.UserRepository;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Compensable(compensationMethod = "updateBalanceByUserIdRollback")
    public String updateBalanceByUserId(@Param("userid") long userid, @Param("balance") double balance) {
        UserEntity userEntity = userRepository.findOne(userid);
        if (userEntity != null) {
            if (userEntity.getBalance() < balance) {
                throw new RuntimeException("Sorry, not sufficient balance under your account.");
            }
        }

        int result = userRepository.updateBalanceByUserId(userid, balance);
        if (result > 0) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }

    public String updateBalanceByUserIdRollback(long userid, double balance) {
        int result = userRepository.updateBalanceByUserId(userid, -balance);
        if (result > 0) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }

    @AutoCompensable
    public String updateBalanceByUserIdAuto(long userid, double balance) {
        UserEntity userEntity = userRepository.findOne(userid);
        if (userEntity != null) {
            if (userEntity.getBalance() < balance) {
                throw new RuntimeException("Sorry, not sufficient balance under your account.");
            }
        }

        int result = userRepository.updateBalanceByUserId(userid, balance);
        if (result > 0) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }
}
