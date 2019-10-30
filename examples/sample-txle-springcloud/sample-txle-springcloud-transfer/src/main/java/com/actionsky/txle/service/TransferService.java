/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.constant.TransferStatusEnum;
import com.actionsky.txle.entity.TransferEntity;
import com.actionsky.txle.repository.TransferRepository;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Service
public class TransferService {
    @Autowired
    private TransferRepository transferRepository;

    @Compensable(compensationMethod = "createTransferRollback")
    public String createTransfer(TransferEntity transferEntity) {
        if (transferRepository.save(transferEntity) == null) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }

    public String createTransferRollback(TransferEntity transferEntity) {
        int result = transferRepository.updateTransferStatusById(transferEntity.getUserid(), transferEntity.getMerchantid(), transferEntity.getAmount(), transferEntity.getPayway(), TransferStatusEnum.Paid.toInteger(), 1, transferEntity.getCreatetime(), TransferStatusEnum.Failed.toInteger());
        if (result == 0) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }

    @AutoCompensable
    public String createTransferAuto(TransferEntity transferEntity) {
        if (transferRepository.save(transferEntity) == null) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }
}
