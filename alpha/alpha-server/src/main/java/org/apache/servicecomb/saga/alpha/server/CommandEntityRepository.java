/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.Command;
import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

public interface CommandEntityRepository extends CrudRepository<Command, Long> {

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.saga.alpha.core.Command c "
      + "SET c.status = :toStatus "
      + "WHERE c.globalTxId = :globalTxId "
      + "  AND c.localTxId = :localTxId "
      + "  AND c.status = :fromStatus")
  void updateStatusByGlobalTxIdAndLocalTxId(
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("globalTxId") String globalTxId,
      @Param("localTxId") String localTxId);

  // 为避免补偿命令从NEW直接更改为DONE。场景：高并发情况，某异常全局事务可能会保存补偿命令后立即保存结束命令(TxConsistentService166-168行)，若该操作在检测补偿和更新补偿状态间发生(EvenScanner142-143行)，则该补偿命令将不被执行。
  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.saga.alpha.core.Command c "
      + "SET c.status = :status "
      + "WHERE c.globalTxId = :globalTxId "
      + "  AND c.localTxId = :localTxId"
      + "  AND c.status = 'PENDING'")
  void updateStatusByGlobalTxIdAndLocalTxId(
      @Param("status") String status,
      @Param("globalTxId") String globalTxId,
      @Param("localTxId") String localTxId);

  List<Command> findByGlobalTxIdAndStatus(String globalTxId, String status);

  // 前接口仅查询没有NEW状态的，就将全局事物结束了，不曾料还有PENDING状态的情况，全局事物也结束了
  @Query("FROM Command T WHERE T.globalTxId = ?1 AND T.status != ?2")
  List<Command> findUncompletedCommandByGlobalTxIdAndStatus(String globalTxId, String status);

  // TODO 2018/1/18 we assumed compensation will never fail. if all service instances are not reachable, we have to set up retry mechanism for pending commands
  @Lock(LockModeType.OPTIMISTIC)
  @Query(value = "SELECT * FROM Command AS c "
      + "WHERE c.eventId IN ("
      + " SELECT MAX(c1.eventId) FROM Command AS c1 "
      + " INNER JOIN Command AS c2 on c1.globalTxId = c2.globalTxId"
      + " WHERE c1.status = 'NEW' "
      + " GROUP BY c1.globalTxId "
      + " HAVING MAX( CASE c2.status WHEN 'PENDING' THEN 1 ELSE 0 END ) = 0) "
//      + "ORDER BY c.eventId ASC LIMIT 1", nativeQuery = true)// 'LIMIT 1' made an effect on performance, and Compensation Command is always executed one by one. So, we canceled 'LIMIT 1'.
      + "ORDER BY c.eventId ASC", nativeQuery = true)
  // 查询某全局事务没有PENDING状态且为NEW状态的Command
  List<Command> findFirstGroupByGlobalTxIdWithoutPendingOrderByIdDesc();

  @Query(value = "SELECT * FROM Command T WHERE T.status = ?1" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<Command> findCommandByStatus(String status);

  @Query(value = "SELECT T.eventId FROM Command T WHERE T.eventId IN ?1")
  Set<Long> findExistCommandList(Set<Long> eventIdList);
}
