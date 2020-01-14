/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.repository;

import com.actionsky.txle.entity.UserEntity;
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
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity T SET T.balance = T.balance - :balance WHERE id = :userId AND T.balance >= :balance")
    int updateBalanceByUserId(@Param("userId") long userId, @Param("balance") double balance);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO txle_sample_user VALUES (?1, 'Gannalyo', ?2, 1, now(), '')", nativeQuery = true)
    int insert(@Param("userId") long userId, @Param("balance") double balance);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO txle_sample_user SELECT * FROM txle_sample_user_copy", nativeQuery = true)
    int insert();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM txle_sample_user WHERE id = ?1", nativeQuery = true)
    int deleteById(@Param("userId") long userId);
}
