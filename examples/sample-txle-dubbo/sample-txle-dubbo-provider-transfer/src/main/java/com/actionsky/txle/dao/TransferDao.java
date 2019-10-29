/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.dao;

import com.actionsky.txle.entity.TransferEntity;
import org.apache.ibatis.annotations.*;

import java.util.Date;

@Mapper
public interface TransferDao {

    @Select("SELECT * FROM txle_sample_transfer T WHERE T.id = #{transferId}")
    TransferEntity findOne(long transferId);

    @Insert("INSERT INTO txle_sample_transfer(userid, merchantid, amount, payway, status, version, createtime) VALUES(#{userid}, #{merchantid}, #{amount}, #{payway}, #{status}, #{version}, #{createtime})")
    int save(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("status") int status, @Param("version") int version, @Param("createtime") Date createtime);

    @Update("UPDATE txle_sample_transfer T SET T.status = #{newStatus} WHERE userid = #{userid} AND merchantid = #{merchantid} AND amount = #{amount} AND payway = #{payway} AND status = #{status} AND version = #{version} AND unix_timestamp(createtime) = unix_timestamp(#{createtime})")
    int updateTransferStatus(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("status") int status, @Param("version") int version, @Param("createtime") Date createtime, @Param("newStatus") int newStatus);
}