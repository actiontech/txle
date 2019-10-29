/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.service;

import org.springframework.data.repository.query.Param;

import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/4/3
 */
public interface TransferService {

    int createTransfer(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("createtime") Date createtime);

    int createTransferAuto(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("createtime") Date createtime);

}
