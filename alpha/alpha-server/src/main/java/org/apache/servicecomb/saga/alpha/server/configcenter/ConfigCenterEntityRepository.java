package org.apache.servicecomb.saga.alpha.server.configcenter;

import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ConfigCenterEntityRepository extends CrudRepository<ConfigCenter, Long> {

    @Query("SELECT T FROM ConfigCenter T WHERE T.instanceid IS NULL AND T.status = ?1")
    List<ConfigCenter> selectConfigCenterList(int status);

    // some client and global config.
    @Query("SELECT T FROM ConfigCenter T WHERE T.status = ?3 AND ((T.instanceid = ?1 AND (T.category = ?2 OR T.category IS NULL)) OR T.instanceid IS NULL)")
    List<ConfigCenter> selectConfigCenterList(String instanceId, String category, int status);

    @Query("SELECT T FROM ConfigCenter T WHERE T.instanceid IS NULL AND T.status = ?1 AND T.type = ?2")
    ConfigCenter selectGlobalConfigCenterByType(int status, int type);

    // some client and global config.
    @Query("SELECT T FROM ConfigCenter T WHERE T.status = ?3 AND T.type = ?4 AND ((T.instanceid = ?1 AND (T.category = ?2 OR T.category IS NULL)) OR T.instanceid IS NULL)")
    List<ConfigCenter> selectConfigCenterByType(String instanceId, String category, int status, int type);

    // some client and global config.
    @Query("SELECT T FROM ConfigCenter T WHERE T.status = ?3 AND T.type >= 50 AND ((T.instanceid = ?1 AND (T.category = ?2 OR T.category IS NULL)) OR T.instanceid IS NULL)")
    List<ConfigCenter> selectClientConfigCenterList(String instanceId, String category, int status);

    @Query("FROM ConfigCenter T WHERE T.status = ?1")
    List<ConfigCenter> findConfigList(Pageable pageable, int status);

    @Query("FROM ConfigCenter T WHERE T.status = ?1 AND FUNCTION('CONCAT_WS', ',', T.servicename, T.instanceid, FUNCTION('TXLE_DECODE', 'config-center-type', T.type), FUNCTION('TXLE_DECODE', 'config-center-status', T.status)," +
            " FUNCTION('TXLE_DECODE', 'config-center-ability', T.ability), FUNCTION('TXLE_DECODE', 'config-center-value', T.value), T.remark, T.updatetime) LIKE CONCAT('%', ?1, '%')")
    List<ConfigCenter> findConfigList(Pageable pageable, int status, String searchText);

    @Query("SELECT COUNT(1) FROM ConfigCenter T")
    long findConfigCount();

    @Query("SELECT COUNT(1) FROM ConfigCenter T WHERE FUNCTION('CONCAT_WS', ',', T.servicename, T.instanceid, FUNCTION('TXLE_DECODE', 'config-center-type', T.type), FUNCTION('TXLE_DECODE', 'config-center-status', T.status)," +
            " FUNCTION('TXLE_DECODE', 'config-center-ability', T.ability), FUNCTION('TXLE_DECODE', 'config-center-value', T.value), T.remark, T.updatetime) LIKE CONCAT('%', ?1, '%')")
    long findConfigCount(String searchText);

}
