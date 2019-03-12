package org.apache.servicecomb.saga.alpha.server.configcenter;

import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ConfigCenterEntityRepository extends CrudRepository<ConfigCenter, Long> {

    @Query("SELECT T FROM ConfigCenter T WHERE T.instanceid IS NULL AND T.status = ?1")
    List<ConfigCenter> selectConfigCenterList(int status);

    // some client and global config.
    @Query("SELECT T FROM ConfigCenter T WHERE T.status = ?2 AND (T.instanceid = ?1 OR T.instanceid IS NULL)")
    List<ConfigCenter> selectConfigCenterList(String instanceId, int status);

    @Query("SELECT T FROM ConfigCenter T WHERE T.instanceid IS NULL AND T.status = ?1 AND T.type = ?2")
    ConfigCenter selectGlobalConfigCenterByType(int status, int type);

    // some client and global config.
    @Query("SELECT T FROM ConfigCenter T WHERE T.status = ?2 AND T.type = ?3 AND (T.instanceid = ?1 OR T.instanceid IS NULL)")
    List<ConfigCenter> selectConfigCenterByType(String instanceId, int status, int type);

    // some client and global config.
    @Query("SELECT T FROM ConfigCenter T WHERE T.status = ?2 AND T.type >= 50 AND (T.instanceid = ?1 OR T.instanceid IS NULL)")
    List<ConfigCenter> selectClientConfigCenterList(String instanceId, int status);

}
