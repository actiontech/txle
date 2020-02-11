/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.entity.UserEntity;
import com.actionsky.txle.repository.UserRepository;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.util.Random;

/**
 * @author Gannalyo
 * @since 2020/2/11
 */
@Service
public class ComplexScenarioUserService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @AutoCompensable
    public int complexUpdateUserAuto(@Param("userId") long userId, @Param("balance") double balance) {
        UserEntity userEntity = userRepository.findOne(userId);
        if (userEntity != null) {
            if (userEntity.getBalance() < balance) {
                throw new RuntimeException("Sorry, not sufficient balance under your account.");
            }
        }

        if (userId == 0) {
            UserEntity user = new UserEntity("xiongjiujiu", 1000000, 1);
            if (userRepository.save(user) == null) {
                throw new RuntimeException("Failed to add user. user = " + user.toJsonString());
            }
        } else if (userId > 1000) {
            if (userRepository.deleteById(userId) < 1) {
                throw new RuntimeException("Failed to delete user. id = " + userId);
            }
        } else {
            // 某子业务中执行多条数据库语句也可以都被补偿
            userRepository.save(new UserEntity("xiongjiujiu", 1000000, 1));
            userRepository.updateBalanceByUserId(userId, balance);
            if (userRepository.updateBalanceByUserId(userId, balance) < 1) {
                throw new RuntimeException("Failed to update user. userId = " + userId);
            }
        }
        return 1;
    }

}
