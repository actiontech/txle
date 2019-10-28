/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.repository;

import com.actionsky.txle.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, Long> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TransferEntity T SET T.status = :newStatus WHERE userid = :userid AND merchantid = :merchantid AND amount = :amount AND payway = :payway AND status = :status AND version = :version AND unix_timestamp(createtime) = unix_timestamp(:createtime)")
    int updateTransferStatusById(@Param("userid") long userid, @Param("merchantid") long merchantid, @Param("amount") double amount, @Param("payway") int payway, @Param("status") int status, @Param("version") int version, @Param("createtime") Date createtime, @Param("newStatus") int newStatus);
}
