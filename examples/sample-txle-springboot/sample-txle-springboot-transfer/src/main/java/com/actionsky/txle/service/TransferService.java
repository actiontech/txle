package com.actionsky.txle.service;

import com.actionsky.txle.constant.TransferStatusEnum;
import com.actionsky.txle.entity.TransferEntity;
import com.actionsky.txle.repository.TransferRepository;
import com.google.gson.JsonObject;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandleType;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.ClientAccidentHandlingService;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
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
public class TransferService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private TransferRepository transferRepository;
    @Autowired
    private ClientAccidentHandlingService clientAccidentHandlingService;
    @Autowired(required = false)
    private OmegaContext omegaContext;

    @Transactional
    @Compensable(compensationMethod = "createTransferRollback")
    public boolean createTransfer(@Param("transferEntity") TransferEntity transferEntity) {
        return transferRepository.save(transferEntity) != null;
    }

    public boolean createTransferRollback(@Param("transferEntity") TransferEntity transferEntity) {
        try {
            return transferRepository.updateTransferStatusById(transferEntity.getUserid(), transferEntity.getMerchantId(), transferEntity.getAmount(), transferEntity.getPayway(),
                    transferEntity.getStatus(), transferEntity.getVersion(), transferEntity.getCreatetime(), TransferStatusEnum.Failed.toInteger()) > 0;
        } catch (Exception e) {
            JsonObject jsonParams = new JsonObject();
            jsonParams.addProperty("type", AccidentHandleType.ROLLBACK_ERROR.toInteger());
            jsonParams.addProperty("globaltxid", omegaContext.globalTxId());
            jsonParams.addProperty("localtxid", omegaContext.localTxId());
            TransferEntity userEntity = transferRepository.findOne(transferEntity.getId());
            if (userEntity != null) {
                jsonParams.addProperty("bizinfo", transferEntity.toJsonString());
            } else {
                jsonParams.addProperty("bizinfo", "{\"transferid\": " + transferEntity.getId() + "}");
            }
            LOG.error("Failed to execute method 'createTransferRollback', params [{}].", jsonParams.toString(), e);
            clientAccidentHandlingService.reportMsgToAccidentPlatform(jsonParams.toString());
            return false;
        }
    }

    @Transactional
    @AutoCompensable
    public boolean createTransferAuto(@Param("transferEntity") TransferEntity transferEntity) {
        return transferRepository.save(transferEntity) != null;
    }
}
