/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.Command;
import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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

  // Avoid the Command status is updated from 'NEW' to 'DONE' directly. If not, it would be not compensated.
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

  @Query("FROM Command T WHERE T.globalTxId = ?1 AND T.status != ?2")
  List<Command> findUncompletedCommandByGlobalTxIdAndStatus(String globalTxId, String status);

  @Query(value = "SELECT * FROM Command T WHERE T.status = ?1" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<Command> findCommandByStatus(String status);

  @Query(value = "SELECT T.eventId FROM Command T WHERE T.eventId IN ?1")
  Set<Long> findExistCommandList(Set<Long> eventIdList);
}
