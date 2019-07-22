package org.apache.servicecomb.saga.alpha.core.configcenter;

import org.apache.servicecomb.saga.common.ConfigCenterType;

import java.util.List;
import java.util.Map;

public interface IConfigCenterService {
    List<ConfigCenter> selectConfigCenterList();

    List<ConfigCenter> selectConfigCenterList(String instanceId, String category);

    List<ConfigCenter> selectHistoryConfigCenterList();

    List<Map<String, String>> selectAllClientIdAndName();

    boolean isEnabledTx(String instanceId, String category, ConfigCenterType type);

    boolean createConfigCenter(ConfigCenter config);

    boolean updateConfigCenter(ConfigCenter config);

    boolean deleteConfigCenter(long id);

    List<ConfigCenter> selectClientConfigCenterList(String instanceId, String category);

    List<ConfigCenter> selectConfigCenterByType(String instanceId, String category, int status, int type);

    List<Map<String, Object>> findConfigList(int pageIndex, int pageSize, String orderName, String direction, String searchText);

    long findConfigCount(String searchText);

    ConfigCenter findOne(long id);
}
