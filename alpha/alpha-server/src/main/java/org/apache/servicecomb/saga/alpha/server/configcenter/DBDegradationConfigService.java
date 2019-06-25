package org.apache.servicecomb.saga.alpha.server.configcenter;

import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.server.restapi.CacheRestApi;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Enumeration;
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
        String configKey = instanceId + "_" + ConfigCenterStatus.Normal.toInteger() + "_" + type.toInteger();
        Enumeration<String> configKeys = CacheRestApi.enabledConfigMap.keys();
        while (configKeys.hasMoreElements()) {
            if (configKey.equals(configKeys.nextElement())){
                return CacheRestApi.enabledConfigMap.get(configKey);
            }
        }

        List<ConfigCenter> configCenterList = configCenterEntityRepository.selectConfigCenterByType(instanceId, ConfigCenterStatus.Normal.toInteger(), type.toInteger());
        if (configCenterList != null && !configCenterList.isEmpty()) {
            String value = "";
            boolean isExistsCongis = false;
            for (ConfigCenter config : configCenterList) {
                if (config.getInstanceid() == null || config.getInstanceid().trim().length() == 0) {
                    isExistsCongis = true;
                    if (config.getAbility() == UtxConstants.NO) {
                        return false;
                    }
                    value = config.getValue();
                    break;
                }
            }
            if (isExistsCongis) {// 强制每个配置必须有全局配置才生效
                for (ConfigCenter config : configCenterList) {
                    if (config.getInstanceid() != null && config.getInstanceid().trim().length() > 0) {
                        if (config.getAbility() == UtxConstants.NO) {
                            break;// do not cover the value of global config.
                        }
                        value = config.getValue();
                        break;// cover the value of global config.
                    }
                }
                CacheRestApi.enabledConfigMap.put(configKey, UtxConstants.ENABLED.equals(value));
                return UtxConstants.ENABLED.equals(value);
            }
        }

        // All of configs except fault-tolerant are enabled by default.
        if (ConfigCenterType.GlobalTxFaultTolerant.equals(type) || ConfigCenterType.CompensationFaultTolerant.equals(type) || ConfigCenterType.AutoCompensationFaultTolerant.equals(type)) {
            CacheRestApi.enabledConfigMap.put(configKey, false);
            return false;
        }
        CacheRestApi.enabledConfigMap.put(configKey, true);
        return true;
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
