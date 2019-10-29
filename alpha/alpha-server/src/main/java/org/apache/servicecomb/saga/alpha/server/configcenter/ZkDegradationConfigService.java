/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.configcenter;

import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;

import java.util.List;
import java.util.Map;

/**
 * @author Gannalyo
 * @since 2019/2/21
 */
public class ZkDegradationConfigService implements IConfigCenterService {
    @Override
    public List<ConfigCenter> selectConfigCenterList() {
        return null;
    }

    @Override
    public List<ConfigCenter> selectConfigCenterList(String instanceId, String category) {
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
    public boolean isEnabledConfig(String instanceId, String category, ConfigCenterType type) {
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
    public List<ConfigCenter> selectClientConfigCenterList(String instanceId, String category) {
        return null;
    }

    @Override
    public List<ConfigCenter> selectConfigCenterByType(String instanceId, String category, int status, int type) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findConfigList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
        return null;
    }

    @Override
    public long findConfigCount(String searchText) {
        return 0;
    }

    @Override
    public ConfigCenter findOne(long id) {
        return null;
    }
}
