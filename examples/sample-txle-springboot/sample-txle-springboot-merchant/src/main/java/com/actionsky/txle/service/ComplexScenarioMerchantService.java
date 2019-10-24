package com.actionsky.txle.service;

import com.actionsky.txle.entity.MerchantEntity;
import com.actionsky.txle.repository.MerchantRepository;
import com.google.gson.JsonObject;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandleType;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.ClientAccidentHandlingService;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Service
public class ComplexScenarioMerchantService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private ClientAccidentHandlingService clientAccidentHandlingService;
    @Autowired(required = false)
    private OmegaContext omegaContext;

    @Transactional
    @Compensable(retries = 3, compensationMethod = "updateBalanceByMerchantIdRollback")
    public int updateBalanceByIdRetry(@Param("merchantid") long merchantid, @Param("balance") double balance) {
        LOG.error("Executing method 'updateBalanceByMerchantIdRetry'.");
        int result = merchantRepository.updateBalanceById(merchantid, balance);
        if (balance == 2) {
            throw new RuntimeException("Retry, can not deduct money");
        }

        return result;
    }

    @Transactional
    @Compensable(timeout = 1, compensationMethod = "updateBalanceByMerchantIdRollback")
    public int updateBalanceByIdTimeout(@Param("merchantid") long merchantid, @Param("balance") double balance) {
        try {
            LOG.error("Executing method 'updateBalanceByMerchantIdTimeout'.");
            int result = merchantRepository.updateBalanceById(merchantid, balance);
            if (balance == 2) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                throw new RuntimeException("测试超时并报异常情况，会多回滚一次。。。。");
            }
            return result;
        } catch (Exception e) {
            LOG.error("Failed to execute method 'ComplexScenarioMerchantService.updateBalanceByMerchantId_Timeout'.", e);
            throw e;
        }
    }

    public int updateBalanceByMerchantIdRollback(@Param("merchantid") long merchantid, @Param("balance") double balance) {
        try {
            LOG.error("Executing complex method 'updateBalanceByMerchantIdRollback'.");
            int result = merchantRepository.updateBalanceById(merchantid, -balance);
            if (result < 1) {
                LOG.error("Failed to execute method 'merchantRepository.updateBalanceByMerchantId(merchantid, -balance)'.");
            }
            return result;
        } catch (Exception e) {
            JsonObject jsonParams = new JsonObject();
            try {
                jsonParams.addProperty("type", AccidentHandleType.ROLLBACK_ERROR.toInteger());
                jsonParams.addProperty("globaltxid", omegaContext.globalTxId());
                jsonParams.addProperty("localtxid", omegaContext.localTxId());
                jsonParams.addProperty("remark", "Failed to execute Compensable SQL [UPDATE MerchantEntity T SET T.balance = T.balance + " + balance + " WHERE id = " + merchantid + "].");
                MerchantEntity merchantEntity = null;
                try {
                    merchantEntity = merchantRepository.findOne(merchantid);
                } catch (Exception e1) {
                    // 如果被补偿的接口是因为数据库连接数过大等数据库原因，那么此处findOne方法也很可能执行失败，所以捕获下
                    LOG.error("Failed to execute complex method 'merchantRepository.findOne([{}])', params [{}].", merchantid, jsonParams.toString(), e1);
                }
                if (merchantEntity != null) {
                    jsonParams.addProperty("bizinfo", merchantEntity.toJsonString());
                } else {
                    jsonParams.addProperty("bizinfo", "{\"merchantid\": " + merchantid + "}");
                }
                LOG.error("Failed to execute complex method 'updateBalanceByMerchantIdRollback', params [{}].", jsonParams.toString(), e);
                clientAccidentHandlingService.reportMsgToAccidentPlatform(jsonParams.toString());
            } catch (Exception e2) {
                LOG.error("Failed to report accident for complex method 'updateBalanceByMerchantIdRollback', params [{}].", jsonParams.toString(), e2);
            }
            // 不要抛出异常，否则org.apache.servicecomb.saga.omega.context.CompensationContext中报(IllegalAccessException | InvocationTargetException)错误
        }
        return 0;
    }

}
