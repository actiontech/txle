package org.apache.servicecomb.saga.alpha.server.configcenter;

import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.server.restapi.CacheRestApi;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * @author Gannalyo
 * @since 2019/2/21
 */
public class DBDegradationConfigService implements IConfigCenterService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ConfigCenterEntityRepository configCenterEntityRepository;

    @Autowired
    private IDataDictionaryService dataDictionaryService;

    @Autowired
    private CacheRestApi cacheRestApi;

    public DBDegradationConfigService(ConfigCenterEntityRepository configCenterEntityRepository) {
        this.configCenterEntityRepository = configCenterEntityRepository;
    }

    @Override
    public List<ConfigCenter> selectConfigCenterList() {
        return configCenterEntityRepository.selectConfigCenterList(ConfigCenterStatus.Normal.toInteger());
    }

    @Override
    public List<ConfigCenter> selectConfigCenterList(String instanceId, String category) {
        return configCenterEntityRepository.selectConfigCenterList(instanceId, category, ConfigCenterStatus.Normal.toInteger());
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
    public boolean isEnabledConfig(String instanceId, String category, ConfigCenterType type) {
        String configKey = TxleConstants.constructConfigCacheKey(instanceId, category, type.toInteger());
        Enumeration<String> configKeys = cacheRestApi.keys();
        while (configKeys.hasMoreElements()) {
            if (configKey.equals(configKeys.nextElement())) {
                return cacheRestApi.get(configKey);
            }
        }

        List<ConfigCenter> configCenterList = configCenterEntityRepository.selectConfigCenterByType(instanceId, category, ConfigCenterStatus.Normal.toInteger(), type.toInteger());
        if (configCenterList != null && !configCenterList.isEmpty()) {
            /**
             * 配置检测逻辑：
             * 1.先判断是否有某配置类型的全局配置，即instanceId为null的，如果有全局配置则验证当前配置是否开启，如果ability=0，则直接返回false，否则标记isExistsConfigs=true，并暂存value
             * 2.
             */
            String value = "";
            boolean isExistsConfigs = false;
            // 1.获取当前类型配置的全局值
            for (ConfigCenter config : configCenterList) {
                if (config.getInstanceid() == null || config.getInstanceid().trim().length() == 0) {
                    if (config.getAbility() == TxleConstants.NO) {
                        return false;
                    }
                    isExistsConfigs = true;
                    value = config.getValue();
                    break;
                }
            }
            // 强制每个配置必须有全局配置才生效
            if (isExistsConfigs) {
                isExistsConfigs = false;
                // 2.获取当前配置的具体值(instanceId和category都为有效值的情况)
                for (ConfigCenter config : configCenterList) {
                    if (config.getInstanceid() != null && config.getInstanceid().trim().length() > 0 && config.getCategory() != null && config.getCategory().trim().length() > 0) {
                        isExistsConfigs = true;
                        if (config.getAbility() == TxleConstants.NO) {
                            // do not cover the value of global config.
                            break;
                        }
                        value = config.getValue();
                        // cover the value of global config.
                        break;
                    }
                }
                // 3.如果未获取到当前配置的具体值，则获取当前配置的默认值(instanceId为有效值、category为无效值的情况)
                if (!isExistsConfigs) {
                    for (ConfigCenter config : configCenterList) {
                        if (config.getInstanceid() != null && config.getInstanceid().trim().length() > 0 && (config.getCategory() == null || config.getCategory().trim().length() == 0)) {
                            if (config.getAbility() == TxleConstants.NO) {
                                // do not cover the value of global config.
                                break;
                            }
                            value = config.getValue();
                            // cover the value of global config.
                            break;
                        }
                    }
                }
                boolean result = TxleConstants.ENABLED.equals(value);
                cacheRestApi.putForDistributedCache(configKey, result);
                return result;
            }
        }

        // All of configs except fault-tolerant are enabled by default.
        if (ConfigCenterType.PauseGlobalTx.equals(type) || ConfigCenterType.GlobalTxFaultTolerant.equals(type) || ConfigCenterType.CompensationFaultTolerant.equals(type) || ConfigCenterType.AutoCompensationFaultTolerant.equals(type)) {
            cacheRestApi.putForDistributedCache(configKey, false);
            return false;
        }
        cacheRestApi.putForDistributedCache(configKey, true);
        return true;
    }

    @Override
    public boolean createConfigCenter(ConfigCenter config) {
        return createOrUpdateConfig(config);
    }

    @Override
    public boolean updateConfigCenter(ConfigCenter config) {
        ConfigCenter existsConfig = configCenterEntityRepository.findOne(config.getId());
        if (existsConfig != null) {
            return createOrUpdateConfig(config);
        }
        return false;
    }

    private boolean createOrUpdateConfig(ConfigCenter config) {
        config.setUpdatetime(new Date());
        if ((config.getServicename() + "").length() == 0) {
            config.setServicename(null);
        }
        if ((config.getInstanceid() + "").length() == 0) {
            config.setInstanceid(null);
        }
        if ((config.getCategory() + "").length() == 0) {
            config.setCategory(null);
        }
        cacheRestApi.putForDistributedCache(TxleConstants.constructConfigCacheKey(config.getInstanceid(), config.getCategory(), config.getType()), TxleConstants.ENABLED.equals(config.getValue()));
        return configCenterEntityRepository.save(config) != null;
    }

    @Override
    public boolean deleteConfigCenter(long id) {
        ConfigCenter config = configCenterEntityRepository.findOne(id);
        if (config != null) {
            configCenterEntityRepository.delete(id);
            cacheRestApi.removeForDistributedCache(TxleConstants.constructConfigCacheKey(config.getInstanceid(), config.getCategory(), config.getType()));
        }
        return true;
    }

    @Override
    public List<ConfigCenter> selectClientConfigCenterList(String instanceId, String category) {
        return configCenterEntityRepository.selectClientConfigCenterList(instanceId, category, ConfigCenterStatus.Normal.toInteger());
    }

    @Override
    public List<ConfigCenter> selectConfigCenterByType(String instanceId, String category, int status, int type) {
        return configCenterEntityRepository.selectConfigCenterByType(instanceId, category, status, type);
    }

    @Override
    public List<Map<String, Object>> findConfigList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
        List<ConfigCenter> configList = this.searchConfigList(pageIndex, pageSize, orderName, direction, searchText);
        if (configList != null && !configList.isEmpty()) {
            List<Map<String, Object>> resultAccidentList = new LinkedList<>();

            Map<String, String> typeValueName = new HashMap<>();
            List<DataDictionaryItem> dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList("config-center-type");
            if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
                dataDictionaryItemList.forEach(dd -> typeValueName.put(dd.getValue(), dd.getName()));
            }

            Map<String, String> statusValueName = new HashMap<>();
            dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList("config-center-status");
            if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
                dataDictionaryItemList.forEach(dd -> statusValueName.put(dd.getValue(), dd.getName()));
            }

            Map<String, String> abilityValueName = new HashMap<>();
            dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList("config-center-ability");
            if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
                dataDictionaryItemList.forEach(dd -> abilityValueName.put(dd.getValue(), dd.getName()));
            }

            configList.forEach(config -> resultAccidentList.add(config.toMap(typeValueName.get(String.valueOf(config.getType())), statusValueName.get(String.valueOf(config.getStatus())), abilityValueName.get(String.valueOf(config.getAbility())))));

            return resultAccidentList;
        }
        return null;
    }

    private List<ConfigCenter> searchConfigList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
        try {
            pageIndex = pageIndex < 1 ? 0 : pageIndex;
            pageSize = pageSize < 1 ? 100 : pageSize;

            Sort.Direction sd = Sort.Direction.DESC;
            if (orderName == null || orderName.length() == 0) {
                orderName = "updatetime";
            }
            if ("asc".equalsIgnoreCase(direction)) {
                sd = Sort.Direction.ASC;
            }

            PageRequest pageRequest = new PageRequest(pageIndex, pageSize, sd, orderName);
            if (searchText == null || searchText.length() == 0) {
                return configCenterEntityRepository.findConfigList(pageRequest, ConfigCenterStatus.Normal.toInteger());
            }
            return configCenterEntityRepository.findConfigList(pageRequest, ConfigCenterStatus.Normal.toInteger(), searchText);
        } catch (Exception e) {
            LOG.error("Failed to find the list of Config Center. params {pageIndex: [{}], pageSize: [{}], orderName: [{}], direction: [{}], searchText: [{}]}.", pageIndex, pageSize, orderName, direction, searchText, e);
        }
        return null;
    }

    @Override
    public long findConfigCount(String searchText) {
        if (searchText == null || searchText.length() == 0) {
            return configCenterEntityRepository.findConfigCount();
        }
        return configCenterEntityRepository.findConfigCount(searchText);
    }

    @Override
    public ConfigCenter findOne(long id) {
        return configCenterEntityRepository.findOne(id);
    }
}
