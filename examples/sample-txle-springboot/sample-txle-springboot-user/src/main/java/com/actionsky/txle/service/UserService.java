/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.entity.UserEntity;
import com.actionsky.txle.repository.UserRepository;
import com.google.gson.JsonObject;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.KafkaMessage;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandleType;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.ClientAccidentHandlingService;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Service
public class UserService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClientAccidentHandlingService clientAccidentHandlingService;
    @Autowired(required = false)
    private OmegaContext omegaContext;

    @Autowired
    private MessageSender messageSender;

    @Value("${spring.datasource.driver-class-name:\"\"}")
    private String drivername;

    @Value("${spring.datasource.url:\"\"}")
    private String dburl;

    @Value("${spring.datasource.username:\"\"}")
    private String dbusername;

    @Transactional
    @Compensable(compensationMethod = "updateBalanceByUserIdRollback")
    public int updateBalanceByUserId(@Param("userId") long userId, @Param("balance") double balance) {
        LOG.error("Executing method 'updateBalanceByUserId'.");
        UserEntity userEntity = userRepository.findOne(userId);
        // 手动补偿场景：由业务人员自行收集所影响的数据信息
        messageSender.reportMessageToServer(new KafkaMessage(drivername, dburl, dbusername, "txle_sample_user", "update", userId + ""));
        if (userEntity != null) {
            if (userEntity.getBalance() < balance) {
                throw new RuntimeException("Sorry, not sufficient balance under your account.");
            }
        }
        return userRepository.updateBalanceByUserId(userId, balance);
    }

    public int updateBalanceByUserIdRollback(@Param("userId") long userId, @Param("balance") double balance) {
        try {
            LOG.error("Executing method 'updateBalanceByUserIdRollback'.");
            int result = userRepository.updateBalanceByUserId(userId, -balance);
            if (result < 1) {
                LOG.error("Failed to execute 'userRepository.updateBalanceByUserId(userId, -balance)'.");
            }
            return result;
        } catch (Exception e) {
            JsonObject jsonParams = new JsonObject();
            try {
                jsonParams.addProperty("type", AccidentHandleType.ROLLBACK_ERROR.toInteger());
                jsonParams.addProperty("globaltxid", omegaContext.globalTxId());
                jsonParams.addProperty("localtxid", omegaContext.localTxId());
                jsonParams.addProperty("remark", "Failed to execute Compensable SQL [UPDATE UserEntity T SET T.balance = T.balance - " + balance + " WHERE id = " + userId + " AND T.balance >= " + balance + "].");
                UserEntity userEntity = null;
                try {
                    userEntity = userRepository.findOne(userId);
                } catch (Exception e1) {
                    // 如果被补偿的接口是因为数据库连接数过大等数据库原因，那么此处findOne方法也会执行失败，所以捕获下
                    LOG.error("Failed to execute method 'userRepository.findOne([{}])', params [{}].", userId, jsonParams.toString(), e1);
                }
                if (userEntity != null) {
                    jsonParams.addProperty("bizinfo", userEntity.toJsonString());
                } else {
                    jsonParams.addProperty("bizinfo", "{\"userid\": " + userId + "}");
                }
                LOG.error("Failed to execute method 'updateBalanceByUserIdRollback', params [{}].", jsonParams.toString(), e);
                clientAccidentHandlingService.reportMsgToAccidentPlatform(jsonParams.toString());
            } catch (Exception e2) {
                LOG.error("Failed to report accident for method 'updateBalanceByUserIdRollback', params [{}].", jsonParams.toString(), e2);
            }
            // 不要抛出异常，否则org.apache.servicecomb.saga.omega.context.CompensationContext中报(IllegalAccessException | InvocationTargetException)错误
        }
        return 0;
    }

    @Transactional
    @AutoCompensable
    public int updateBalanceByUserIdAuto(@Param("userId") long userId, @Param("balance") double balance) {
//        UserEntity userEntity = userRepository.findOne(userId);
//        if (userEntity != null) {
//            if (userEntity.getBalance() < balance) {
//                throw new RuntimeException("Sorry, not sufficient balance under your account.");
//            }
//        }
//        if (userRepository.updateBalanceByUserId(userId, balance) < 1) {
//        userRepository.updateBalanceByUserId(1, balance);
//        userRepository.delete(1002L);
//        userRepository.deleteById(Long.valueOf(new Random().nextInt(1000)));
//        userRepository.delete(userEntity);
        userRepository.insert();
//        if (userRepository.save(new UserEntity("xiongjiujiu", 2, 2)) == null) {
//            throw new RuntimeException("Sorry, not sufficient balance under your account.");
//        }
        return 1;
    }

    @Transactional
    @Compensable(compensationMethod = "highPerformanceRollback")
    public void highPerformance() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        LOG.error("[" + sdf.format(new Date()) + "] Executing method '" + this.getClass() + ".highPerformance'.");
    }

    public void highPerformanceRollback() {
    }
}
