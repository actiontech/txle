/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.repository;

import com.actionsky.txle.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Repository
public interface MerchantRepository extends JpaRepository<MerchantEntity, Long> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MerchantEntity T SET T.balance = T.balance + :balance WHERE id = :merchantid")
    int updateBalanceById(@Param("merchantid") long merchantid, @Param("balance") double balance);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM txle_sample_merchant WHERE id = ?1", nativeQuery = true)
    int deleteById(@Param("merchantid") long merchantid);
}
