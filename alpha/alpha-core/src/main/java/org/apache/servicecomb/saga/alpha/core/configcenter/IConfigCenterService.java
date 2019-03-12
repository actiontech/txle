package org.apache.servicecomb.saga.alpha.core.configcenter;

import org.apache.servicecomb.saga.common.ConfigCenterType;

import java.util.List;
import java.util.Map;

public interface IConfigCenterService {
    List<ConfigCenter> selectConfigCenterList();

    List<ConfigCenter> selectConfigCenterList(String instanceId);

    List<ConfigCenter> selectHistoryConfigCenterList();

    List<Map<String, String>> selectAllClientIdAndName();

    boolean isEnabledTx(String instanceId, ConfigCenterType type);

    boolean createConfigCenter(ConfigCenter config);

    boolean updateConfigCenter(ConfigCenter config);

    boolean deleteConfigCenter(long id);

    List<ConfigCenter> selectClientConfigCenterList(String instanceId);
}
