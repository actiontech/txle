package org.apache.servicecomb.saga.alpha.server.configcenter;

import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

/**
 * @author Gannalyo
 * @date 2019/2/21
 */
public class DBDegradationConfigService implements IConfigCenterService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ConfigCenterEntityRepository configCenterEntityRepository;

    public DBDegradationConfigService(ConfigCenterEntityRepository configCenterEntityRepository) {
        this.configCenterEntityRepository = configCenterEntityRepository;
    }

    @Override
    public List<ConfigCenter> selectConfigCenterList() {
        return configCenterEntityRepository.selectConfigCenterList(ConfigCenterStatus.Normal.toInteger());
    }

    @Override
    public List<ConfigCenter> selectConfigCenterList(String instanceId) {
        return configCenterEntityRepository.selectConfigCenterList(instanceId, ConfigCenterStatus.Normal.toInteger());
    }

    @Override
    public List<ConfigCenter> selectHistoryConfigCenterList() {
        return configCenterEntityRepository.selectConfigCenterList(ConfigCenterStatus.Historical.toInteger());
    }

    @Override
    public List<Map<String, String>> selectAllClientIdAndName() {
        return null;
    }

    @Override
    public boolean isEnabledTx(String instanceId, ConfigCenterType type) {
        List<ConfigCenter> configCenterList = configCenterEntityRepository.selectConfigCenterByType(instanceId, ConfigCenterStatus.Normal.toInteger(), type.toInteger());
        if (configCenterList != null && !configCenterList.isEmpty()) {
            String value = "";
            for (ConfigCenter config : configCenterList) {
                if (config.getInstanceid() == null || config.getInstanceid().trim().length() == 0) {
                    if (config.getAbility() == UtxConstants.NO) {
                        return false;
                    }
                    value = config.getValue();
                    break;
                }
            }
            for (ConfigCenter config : configCenterList) {
                if (config.getInstanceid() != null && config.getInstanceid().trim().length() > 0) {
                    if (config.getAbility() == UtxConstants.NO) {
                        break;// do not cover the value of global config.
                    }
                    value = config.getValue();
                    break;// cover the value of global config.
                }
            }
            return UtxConstants.ENABLED.equals(value);
        }
        return true;// All of configs are enabled by default.
    }

    @Override
    public boolean createConfigCenter(ConfigCenter config) {
        return configCenterEntityRepository.save(config) != null;
    }

    @Override
    public boolean updateConfigCenter(ConfigCenter config) {
        ConfigCenter existsConfig = configCenterEntityRepository.findOne(config.getId());
        if (existsConfig != null) {
            configCenterEntityRepository.save(existsConfig);
            config.setId(null);
        }
        return configCenterEntityRepository.save(config) != null;
    }

    @Override
    public boolean deleteConfigCenter(long id) {
        configCenterEntityRepository.delete(id);
        return true;
    }

    @Override
    public List<ConfigCenter> selectClientConfigCenterList(String instanceId) {
        return configCenterEntityRepository.selectClientConfigCenterList(instanceId, ConfigCenterStatus.Normal.toInteger());
    }
}
