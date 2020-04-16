/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.configcenter;

import com.actionsky.txle.cache.ITxleConsistencyCache;
import org.apache.servicecomb.saga.alpha.core.TxleConsulClient;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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

    @Resource(name = "txleMysqlCache")
    @Autowired
    private ITxleConsistencyCache consistencyCache;

    @Autowired
    private TxleConsulClient consulClient;

    public DBDegradationConfigService(ConfigCenterEntityRepository configCenterEntityRepository) {
        this.configCenterEntityRepository = configCenterEntityRepository;
    }

    @PostConstruct
    void init() {
        new Thread(() -> this.initializeConfigCache()).start();
    }

    private void initializeConfigCache() {
        // delete all
        consistencyCache.deleteAll();

        if (consulClient.isMaster()) {
            // initialize configs from db
            List<ConfigCenter> configCenterList = this.selectConfigCenterList();
            if (configCenterList != null && !configCenterList.isEmpty()) {
                configCenterList.forEach(cfg -> {
                    String configKey = TxleConstants.constructGlobalConfigValueKey(cfg.getInstanceid(), cfg.getCategory(), ConfigCenterType.convertTypeFromValue(cfg.getType()));
                    consistencyCache.setKeyValueCache(configKey, cfg.getAbility() == TxleConstants.YES ? cfg.getValue() : TxleConstants.DISABLED);
                    if (cfg.getInstanceid() == null && cfg.getAbility() == TxleConstants.NO) {
                        consistencyCache.setKeyValueCache(TxleConstants.constructGlobalConfigAbilityKey(cfg.getInstanceid(), cfg.getCategory(), ConfigCenterType.convertTypeFromValue(cfg.getType())), TxleConstants.NO + "");
                    }
                });
            }
        }
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
        return consistencyCache.getBooleanValue(instanceId, category, type);
    }

    @Override
    public boolean createConfigCenter(ConfigCenter config) {
        /**
         * 新增逻辑：
         * 前端验证：新增时状态默认为正常且不允许修改
         *
         * 新增全局配置：验证ability是否为0，如果为0，设置ability=0缓存
         * 新增客户端配置：验证ability是否为0，如果为0，设置ability=0缓存，若无对应全局配置，则先新增对应全局配置
         */
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
        if (config.getInstanceid() == null) {
            if (configCenterEntityRepository.save(config) != null) {
                this.setCurrentTypeCacheForNewConfig(config);
                return true;
            }
        } else {
            // 新增客户端配置时，检测该type是否有全局配置，如果没有则新增该type的全局配置
            List<ConfigCenter> configCenterList = configCenterEntityRepository.selectConfigCenterByType(null, null, ConfigCenterStatus.Normal.toInteger(), config.getType());
            if (configCenterList == null || configCenterList.isEmpty()) {
                configCenterEntityRepository.save(new ConfigCenter(null, null, null, ConfigCenterStatus.Normal, TxleConstants.YES, ConfigCenterType.convertTypeFromValue(config.getType()), config.getValue(), config.getRemark()));
            }
            if (configCenterEntityRepository.save(config) != null) {
                this.setCurrentTypeCacheForNewConfig(config);
                return true;
            }
        }
        return false;
    }

    // set config cache for value and ability
    private void setCurrentTypeCacheForNewConfig(ConfigCenter config) {
        String configKey = TxleConstants.constructGlobalConfigValueKey(config.getInstanceid(), config.getCategory(), ConfigCenterType.convertTypeFromValue(config.getType()));
        boolean result = consistencyCache.setKeyValueCache(configKey, config.getValue());
        if (!result) {
            throw new RuntimeException("Failed to update config cache. configCacheKey = " + configKey);
        }
        // ability缓存默认为yes不缓存，若为no则缓存
        String globalAbilityConfigKey = TxleConstants.constructGlobalConfigAbilityKey(config.getInstanceid(), config.getCategory(), ConfigCenterType.convertTypeFromValue(config.getType()));
        consistencyCache.delete(globalAbilityConfigKey);
        if (config.getAbility() != TxleConstants.YES) {
            result = consistencyCache.setKeyValueCache(globalAbilityConfigKey, TxleConstants.NO + "");
            if (!result) {
                throw new RuntimeException("Failed to update config cache. globalAbilityConfigKey = " + globalAbilityConfigKey);
            }
        }
    }

    @Override
    public boolean updateConfigCenter(ConfigCenter config) {
        ConfigCenter existsConfig = configCenterEntityRepository.findOne(config.getId());
        if (existsConfig != null) {
            /**
             * 更新逻辑：
             * 前端验证：修改全局配置的为非正常状态前，请先修改该类型的客户端配置；类型不允许修改；修改能力时，需提示对客户端配置会有影响
             *
             * 验证ability是否为0，如果为0，设置ability=0缓存
             * 验证status是否为非正常状态，如果是则去除当前缓存
             */
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

            if (configCenterEntityRepository.save(config) != null) {
                this.setCurrentTypeCacheForNewConfig(config);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteConfigCenter(long id) {
        ConfigCenter config = configCenterEntityRepository.findOne(id);
        if (config != null) {
            configCenterEntityRepository.delete(id);
            consistencyCache.delete(TxleConstants.constructGlobalConfigValueKey(config.getInstanceid(), config.getCategory(), ConfigCenterType.convertTypeFromValue(config.getType())));
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
