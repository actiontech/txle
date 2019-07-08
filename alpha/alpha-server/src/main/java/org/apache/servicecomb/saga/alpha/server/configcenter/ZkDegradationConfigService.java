package org.apache.servicecomb.saga.alpha.server.configcenter;

import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;

import java.util.List;
import java.util.Map;

/**
 * @author Gannalyo
 * @date 2019/2/21
 */
public class ZkDegradationConfigService implements IConfigCenterService {
    @Override
    public List<ConfigCenter> selectConfigCenterList() {
        return null;
    }

    @Override
    public List<ConfigCenter> selectConfigCenterList(String instanceId) {
        return null;
    }

    @Override
    public List<ConfigCenter> selectHistoryConfigCenterList() {
        return null;
    }

    @Override
    public List<Map<String, String>> selectAllClientIdAndName() {
        return null;
    }

    @Override
    public boolean isEnabledTx(String instanceId, ConfigCenterType type) {
        return false;
    }

    @Override
    public boolean createConfigCenter(ConfigCenter config) {
        return false;
    }

    @Override
    public boolean updateConfigCenter(ConfigCenter config) {
        return false;
    }

    @Override
    public boolean deleteConfigCenter(long id) {
        return false;
    }

    @Override
    public List<ConfigCenter> selectClientConfigCenterList(String instanceId) {
        return null;
    }

    @Override
    public List<ConfigCenter> selectConfigCenterByType(String instanceId, int status, int type) {
        return null;
    }
}
