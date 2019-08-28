/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import java.util.Date;
import java.util.List;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.apache.servicecomb.saga.alpha.core.TxTimeout;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface TxTimeoutEntityRepository extends CrudRepository<TxTimeout, Long> {

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.saga.alpha.core.TxTimeout t "
      + "SET t.status = :status "
      + "WHERE t.globalTxId = :globalTxId "
      + "  AND t.localTxId = :localTxId")
  void updateStatusByGlobalTxIdAndLocalTxId(
      @Param("status") String status,
      @Param("globalTxId") String globalTxId,
      @Param("localTxId") String localTxId);

  @Lock(LockModeType.OPTIMISTIC)
  @Query("SELECT t FROM TxTimeout AS t "
      + "WHERE t.status = 'NEW' "
      + "  AND t.expiryTime < CURRENT_TIMESTAMP "
      + "ORDER BY t.expiryTime ASC")
  List<TxTimeout> findFirstTimeoutTxOrderByExpireTimeAsc(Pageable pageable);

  @Lock(LockModeType.OPTIMISTIC)
  @Query(value = "SELECT * FROM TxTimeout AS t "
      + "WHERE t.status = 'NEW' "
      + "  AND t.expiryTime < ?1 "
      + "ORDER BY t.expiryTime ASC" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<TxTimeout> findFirstTimeoutTxOrderByExpireTimeAsc(/*Pageable pageable, */Date currentDateTime);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE TxTimeout t "
      + "SET t.status = 'DONE' "
      + "WHERE t.status != 'DONE' AND EXISTS ("
      + "  SELECT t1.globalTxId FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.localTxId = t.localTxId "
      + "    AND t1.type != t.type"
      + ")")
  void updateStatusOfFinishedTx();

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE TxTimeout t SET t.status = 'DONE' WHERE t.status != 'DONE' AND t.surrogateId IN ?1")
  void updateStatusOfFinishedTx(List<Long> surrogateIdList);

  @Query(value = "SELECT t.surrogateId FROM TxTimeout t, TxEvent t1 WHERE t.status != 'DONE' AND t1.globalTxId = t.globalTxId AND t1.localTxId = t.localTxId AND t1.type != t.type" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<Long> selectTimeoutIdList();

  @Query(value = "SELECT * FROM (SELECT count(1) FROM TxTimeout t WHERE t.eventId = ?1) T1", nativeQuery = true)
  long findTxTimeoutByEventId(long eventId);
}
