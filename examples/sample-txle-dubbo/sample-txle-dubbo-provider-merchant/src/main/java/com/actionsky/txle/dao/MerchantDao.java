/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MerchantDao {

    @Update("UPDATE txle_sample_merchant T SET T.balance = T.balance + #{balance} WHERE id = #{merchantid}")
    int updateBalanceById(@Param("merchantid") long merchantid, @Param("balance") double balance);

}