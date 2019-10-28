/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

/**
 * @author Gannalyo
 * @since 2019/4/3
 */
public interface MerchantService {

    int payMoneyToMerchant(long merchantid, double balance);

    int payMoneyToMerchantAuto(long merchantid, double balance);
}
