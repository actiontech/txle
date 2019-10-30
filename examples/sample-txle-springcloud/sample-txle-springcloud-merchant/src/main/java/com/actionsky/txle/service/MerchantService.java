/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import com.actionsky.txle.repository.MerchantRepository;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Service
@Transactional
public class MerchantService {
    @Autowired
    private MerchantRepository merchantRepository;

    @Compensable(compensationMethod = "updateBalanceByMerchantIdRollback")
    public String updateBalanceByMerchantId(long merchantid, double balance) {
        int result = merchantRepository.updateBalanceById(merchantid, balance);
        if (result > 0) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }

    public String updateBalanceByMerchantIdRollback(long merchantid, double balance) {
        int result = merchantRepository.updateBalanceById(merchantid, -balance);
        if (result > 0) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }

    @AutoCompensable
    public String updateBalanceByMerchantIdAuto(long merchantid, double balance) {
        int result = merchantRepository.updateBalanceById(merchantid, balance);
        if (result > 0) {
            return TxleConstants.ERROR;
        }
        return TxleConstants.OK;
    }
}
