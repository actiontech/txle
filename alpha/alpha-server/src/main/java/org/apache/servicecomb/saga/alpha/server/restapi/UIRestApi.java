/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.restapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.gson.GsonBuilder;
import org.apache.servicecomb.saga.alpha.core.*;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.server.TableFieldRepository;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.common.ReturnValue;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;

@RestController
public class UIRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(UIRestApi.class);

    @Autowired
    private HttpServletRequest request;

    private TableFieldRepository tableFieldRepository;

    private TxEventRepository eventRepository;

    @Autowired
    private IConfigCenterService configCenterService;

    @Autowired
    private IDataDictionaryService dataDictionaryService;

    @Autowired
    private TxleMetrics txleMetrics;

    @Autowired
    private IAccidentHandlingService accidentHandlingService;

    @Autowired
    private ITxleCache txleCache;

    public UIRestApi(TableFieldRepository tableFieldRepository, TxEventRepository eventRepository) {
        this.tableFieldRepository = tableFieldRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping(value = "/transactionColumns")
    public ResponseEntity<ReturnValue> findTxEventTableInfo() {
        return getTableColumns("TxEvent", "the table of Global Transaction");
    }

    @GetMapping(value = "/configColumns")
    public ResponseEntity<ReturnValue> findConfigTableInfo() {
        return getTableColumns("Config", "the table of Config Center");
    }

    @GetMapping(value = "/accidentColumns")
    public ResponseEntity<ReturnValue> findAccidentTableInfo() {
        return getTableColumns("Accident", "the table of Accident Handling");
    }

    private ResponseEntity<ReturnValue> getTableColumns(String tableName, String tableDesc) {
        ReturnValue rv = new ReturnValue();
        try {
            List<TableFieldEntity> tableFieldEntityList = tableFieldRepository.selectTableFieldsByName(tableName);
            if (tableFieldEntityList != null && !tableFieldEntityList.isEmpty()) {
                rv.setData(JSONObject.parseArray(JSON.toJSONString(tableFieldEntityList)));
            }
        } catch (Exception e) {
            rv.setMessage("Failed to get columns from " + tableDesc + ".");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }

        return ResponseEntity.ok(rv);
    }

    @GetMapping(value = "/globalTransactions/{pageIndex}/{pageSize}/{orderName}/{direction}")
    public ResponseEntity<ReturnValue> findTxList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String orderName, @PathVariable String direction) {
        return findTxList(pageIndex, pageSize, orderName, direction, "");
    }

    @GetMapping(value = "/globalTransactions/{pageIndex}/{pageSize}/{orderName}/{direction}/{searchText}")
    public ResponseEntity<ReturnValue> findTxList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String orderName, @PathVariable String direction, @PathVariable String searchText) {
        ReturnValue rv = new ReturnValue();
        try {
            // To decrease the value of the variable 'pageIndex' for getting a compatible param which comes from UI.
            List<Map<String, Object>> txStartedEventList = eventRepository.findTxList(--pageIndex, pageSize, convertToEventEntityFieldName(orderName), direction, searchText);
            if (txStartedEventList != null && !txStartedEventList.isEmpty()) {
                List<Map<String, Object>> resultList = new LinkedList<>();
                txStartedEventList.forEach(map -> {
                    Map<String, Object> resultMap = new HashMap<>();
                    map.keySet().forEach(key -> resultMap.put(key.toLowerCase(), map.get(key)));
                    resultList.add(resultMap);
                });
                txStartedEventList.clear();
                rv.setData(JSONObject.parseArray(JSON.toJSONString(resultList, SerializerFeature.WriteMapNullValue)));
                rv.setTotal(eventRepository.findTxCount(searchText));
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find the default list of Global Transaction.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    // All of field names are lower case at front-end, but could not guarantee the format at back-end, so parts of field names need be converted.
    // In fact, the filed names of all tables are lower case, except TxEvent, Command and Timeout.
    private String convertToEventEntityFieldName(String fieldName) {
        if ("surrogateId".equalsIgnoreCase(fieldName)) {
            return "surrogateId";
        }
        if ("serviceName".equalsIgnoreCase(fieldName)) {
            return "serviceName";
        }
        if ("instanceId".equalsIgnoreCase(fieldName)) {
            return "instanceId";
        }
        if ("creationTime".equalsIgnoreCase(fieldName)) {
            return "creationTime";
        }
        if ("globalTxId".equalsIgnoreCase(fieldName)) {
            return "globalTxId";
        }
        if ("localTxId".equalsIgnoreCase(fieldName)) {
            return "localTxId";
        }
        if ("parentTxId".equalsIgnoreCase(fieldName)) {
            return "parentTxId";
        }
        if ("compensationMethod".equalsIgnoreCase(fieldName)) {
            return "compensationMethod";
        }
        if ("expiryTime".equalsIgnoreCase(fieldName)) {
            return "expiryTime";
        }
        if ("retryMethod".equalsIgnoreCase(fieldName)) {
            return "retryMethod";
        }
        return fieldName;
    }

    @PostMapping(value = "/subTransactions")
    public ResponseEntity<ReturnValue> findSubTxList(@RequestBody JSONObject jsonParams) {
        ReturnValue rv = new ReturnValue();
        try {
            if (jsonParams == null) {
                rv.setMessage("The identifications of Global Transactions are empty.");
                return ResponseEntity.badRequest().body(rv);
            }
            String globalTxIds = jsonParams.getString("globalTxIds");
            if (globalTxIds != null && globalTxIds.length() > 0) {
                List<Map<String, Object>> subTxList = eventRepository.findSubTxList(globalTxIds);
                if (subTxList != null && !subTxList.isEmpty()) {
                    List<Map<String, Object>> resultList = new LinkedList<>();
                    subTxList.forEach(map -> {
                        Map<String, Object> resultMap = new HashMap<>();
                        map.keySet().forEach(key -> resultMap.put(key.toLowerCase(), map.get(key)));
                        resultList.add(resultMap);
                    });
                    subTxList.clear();
                    rv.setData(JSONObject.parseArray(JSON.toJSONString(resultList, SerializerFeature.WriteMapNullValue)));
                }
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find the default list of Sub Transaction.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @PostMapping("/pauseGlobalTransactions")
    public ResponseEntity<ReturnValue> pauseGlobalTransactions(@RequestBody JSONObject jsonParams) {
        return saveOperationTxEventWithVerification(jsonParams, jsonParams.getIntValue("pausePeriod"), "pause");
    }

    @PostMapping("/recoverGlobalTransactions")
    public ResponseEntity<ReturnValue> recoverGlobalTransactions(@RequestBody JSONObject jsonParams) {
        return saveOperationTxEventWithVerification(jsonParams, 0, "recover");
    }

    @PostMapping("/terminateGlobalTransactions")
    public ResponseEntity<ReturnValue> terminateGlobalTransactions(@RequestBody JSONObject jsonParams) {
        return saveOperationTxEventWithVerification(jsonParams, 0, "terminate");
    }

    private ResponseEntity<ReturnValue> saveOperationTxEventWithVerification(JSONObject jsonParams, int pausePeriod, String operation) {
        ReturnValue rv = new ReturnValue();
        if (jsonParams == null) {
            rv.setMessage("The identifications of Global Transactions are empty, operation [" + operation + "].");
            return ResponseEntity.badRequest().body(rv);
        }
        String globalTxIds = jsonParams.getString("globalTxIds");
        try {
            if (globalTxIds == null || globalTxIds.trim().length() == 0) {
                rv.setMessage("The identifications of Global Transactions are empty.");
                return ResponseEntity.badRequest().body(rv);
            }

            // ps: The method "remove" of the container will not work according to following code.
            // Arrays.asList(globalTxIds.split(","));
            List<String> globalTxIdList = new ArrayList<>();
            for (String globalTxId : globalTxIds.split(",")) {
                globalTxIdList.add(globalTxId);
            }

            // To filter which have been over.
            List<TxEvent> txEventList = eventRepository.selectTxEventByGlobalTxIds(globalTxIdList);
            if (txEventList == null || txEventList.isEmpty()) {
                rv.setMessage("Selected an empty result by globalTxIds [" + globalTxIds + "].");
                return ResponseEntity.ok(rv);
            }

            AtomicReference<String> operationAdjective = new AtomicReference<>("pause".equals(operation) ? "suspended" : "recover".equals(operation) ? "normal" : "terminated");
            txEventList.forEach(event -> {
                if (SagaEndedEvent.name().equals(event.type())) {
                    globalTxIdList.remove(event.globalTxId());
                } else {
                    List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                    if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                        if ("pause".equals(operation) && pauseContinueEventList.size() % 2 == 1) {
                            globalTxIdList.remove(event.globalTxId());
                        } else if ("recover".equals(operation) && pauseContinueEventList.size() % 2 == 0) {
                            globalTxIdList.remove(event.globalTxId());
                        }
                    }
                }
            });
            if (globalTxIdList.isEmpty()) {
                rv.setMessage("All global transactions have been over or " + operationAdjective.get() + ".");
                return ResponseEntity.ok(rv);
            }

            txEventList.forEach(event -> {
                if (globalTxIdList.contains(event.globalTxId())) {
                    globalTxIdList.remove(event.globalTxId());
                    String ipPort = request.getRemoteAddr() + ":" + request.getRemotePort();
                    String typeName = AdditionalEventType.SagaPausedEvent.name();
                    if ("recover".equals(operation)) {
                        typeName = AdditionalEventType.SagaContinuedEvent.name();
                    } else if ("terminate".equals(operation)) {
                        typeName = EventType.TxAbortedEvent.name();
                    }
                    TxEvent txEvent = new TxEvent(ipPort, ipPort, event.globalTxId(), event.localTxId(), event.parentTxId(), typeName, "", pausePeriod, "", 0, event.category(), null);
                    eventRepository.save(txEvent);
                    if ("terminate".equals(operation)) {
                        // Do not compensate after terminating.
                        TxEvent endedEvent = new TxEvent(event.serviceName(), event.instanceId(), event.globalTxId(), event.globalTxId(), null, SagaEndedEvent.name(), "", event.category(), null);
                        endedEvent.setSurrogateId(null);
                        eventRepository.save(endedEvent);
                    }
                    // Set cache for global transaction status.
                    if ("pause".equals(operation)) {
                        txleCache.putDistributedTxSuspendStatusCache(event.globalTxId(), true, 60);
                    } else {
                        txleCache.removeDistributedTxSuspendStatusCache(event.globalTxId());
                    }
                    txleMetrics.countTxNumber(event, false, false);
                }
            });
        } catch (Exception e) {
            rv.setMessage("Failed to " + operation + " global transactions, param [" + globalTxIds + "].");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/pauseAllGlobalTransactions")
    public ResponseEntity<ReturnValue> pauseAllGlobalTransactions() {
        ReturnValue rv = new ReturnValue();
        try {
            // Check the paused status of global transaction
            final String pauseAllGlobalTxKey = TxleConstants.constructConfigCacheKey(null, null, ConfigCenterType.PauseGlobalTx.toInteger());
            if (txleCache.getConfigCache().getOrDefault(pauseAllGlobalTxKey, false)) {
                return ResponseEntity.ok(rv);
            }

            List<ConfigCenter> configCenterList = configCenterService.selectConfigCenterByType(null, null, ConfigCenterStatus.Normal.toInteger(), ConfigCenterType.PauseGlobalTx.toInteger());
            if (configCenterList != null && !configCenterList.isEmpty()) {
                return ResponseEntity.ok(rv);
            }

            // 1.Construct a global config for paused status.
            String ipPort = request.getRemoteAddr() + ":" + request.getRemotePort();
            configCenterService.createConfigCenter(new ConfigCenter(null, null, null, ConfigCenterStatus.Normal, 1, ConfigCenterType.PauseGlobalTx, "enabled", ipPort + " - pauseAllTransaction"));
            txleCache.putDistributedConfigCache(pauseAllGlobalTxKey, true);

            // 2.Construct a paused event for every global transaction as long as it is not paused and done.
            List<TxEvent> unendedTxEventList = eventRepository.selectUnendedTxEvents(EventScanner.getUnendedMinEventId());
            if (unendedTxEventList != null && !unendedTxEventList.isEmpty()) {
                List<String> globalTxIdList = new ArrayList<>();
                unendedTxEventList.forEach(event -> globalTxIdList.add(event.globalTxId()));

                unendedTxEventList.forEach(event -> {
                    List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                    if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                        TxEvent pausedEvent = pauseContinueEventList.get(0);
                        if (AdditionalEventType.SagaPausedEvent.name().equals(pausedEvent.type()) || AdditionalEventType.SagaAutoContinuedEvent.name().equals(pausedEvent.type())) {
                            globalTxIdList.remove(event.globalTxId());
                        }
                    }
                });
                if (globalTxIdList.isEmpty()) {
                    return ResponseEntity.ok(rv);
                }

                unendedTxEventList.forEach(event -> {
                    if (globalTxIdList.contains(event.globalTxId())) {
                        TxEvent pausedEvent = new TxEvent(ipPort, ipPort, event.globalTxId(), event.localTxId(), event.parentTxId(), AdditionalEventType.SagaPausedEvent.name(), "", 0, "", 0, event.category(), null);
                        eventRepository.save(pausedEvent);
                        txleMetrics.countTxNumber(event, false, false);
                    }
                });
            }
        } catch (Exception e) {
            rv.setMessage("Failed to pause all global transactions.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/recoverAllGlobalTransactions")
    public ResponseEntity<ReturnValue> recoverAllGlobalTransactions() {
        ReturnValue rv = new ReturnValue();
        try {
            // 1.Remove the global config for paused transaction
            List<ConfigCenter> configCenterList = configCenterService.selectConfigCenterByType(null, null, ConfigCenterStatus.Normal.toInteger(), ConfigCenterType.PauseGlobalTx.toInteger());
            if (configCenterList != null && !configCenterList.isEmpty()) {
                ConfigCenter configCenter = configCenterList.get(0);
                configCenter.setStatus(ConfigCenterStatus.Historical.toInteger());
                configCenterService.updateConfigCenter(configCenter);
            }

            // 2.Remove the paused event for every global transaction. in fact, add a recovery event.
            List<TxEvent> unendedTxEventList = eventRepository.selectUnendedTxEvents(EventScanner.getUnendedMinEventId());
            if (unendedTxEventList != null && !unendedTxEventList.isEmpty()) {
                List<String> globalTxIdList = new ArrayList<>();
                unendedTxEventList.forEach(event -> globalTxIdList.add(event.globalTxId()));

                unendedTxEventList.forEach(event -> {
                    List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                    if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                        if (pauseContinueEventList.size() % 2 == 0) {
                            globalTxIdList.remove(event.globalTxId());
                        }
                    }
                });
                if (globalTxIdList.isEmpty()) {
                    return ResponseEntity.ok(rv);
                }

                String ipPort = request.getRemoteAddr() + ":" + request.getRemotePort();
                unendedTxEventList.forEach(event -> {
                    if (globalTxIdList.contains(event.globalTxId())) {
                        TxEvent continuedEvent = new TxEvent(ipPort, ipPort, event.globalTxId(), event.localTxId(), event.parentTxId(), AdditionalEventType.SagaContinuedEvent.name(), "", 0, "", 0, event.category(), null);
                        eventRepository.save(continuedEvent);
                        txleMetrics.countTxNumber(event, false, false);
                    }
                });
            }
        } catch (Exception e) {
            rv.setMessage("Failed to recover all global transactions.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        } finally {
            txleCache.removeDistributedConfigCache(TxleConstants.constructConfigCacheKey(null, null, ConfigCenterType.PauseGlobalTx.toInteger()));
            txleCache.getTxSuspendStatusCache().clear();
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/degradeGlobalTransaction")
    public ResponseEntity<ReturnValue> degradeGlobalTransaction() {
        ReturnValue rv = new ReturnValue();
        try {
            // Business program will only run without global transaction in case of degrading, even though business program is running. As we all know, downgrading is meant to keep business running.
            boolean enabledTx = configCenterService.isEnabledConfig(null, null, ConfigCenterType.GlobalTx);
            if (!enabledTx) {
                rv.setMessage("Sever has been degraded for the Global Transaction.");
                return ResponseEntity.ok(rv);
            }
            String ipPort = request.getRemoteAddr() + ":" + request.getRemotePort();
            boolean enabled = configCenterService.createConfigCenter(new ConfigCenter(null, null, null, ConfigCenterStatus.Normal, 1, ConfigCenterType.GlobalTx, "disabled", ipPort + " - degradeGlobalTransaction"));
            if (!enabled) {
                rv.setMessage("Failed to save the degradation configuration of global transaction.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
            } else {
                txleCache.putDistributedConfigCache(TxleConstants.constructConfigCacheKey(null, null, ConfigCenterType.GlobalTx.toInteger()), false);
            }
        } catch (Exception e) {
            rv.setMessage("Failed to degrade global transaction.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/startGlobalTransaction")
    public ResponseEntity<ReturnValue> startGlobalTransaction() {
        ReturnValue rv = new ReturnValue();
        try {
            List<ConfigCenter> configCenterList = configCenterService.selectConfigCenterByType(null, null, ConfigCenterStatus.Normal.toInteger(), ConfigCenterType.GlobalTx.toInteger());
            if (configCenterList == null || configCenterList.isEmpty()) {
                rv.setMessage("Sever has been started for the Global Transaction.");
                txleCache.removeDistributedConfigCache(TxleConstants.constructConfigCacheKey(null, null, ConfigCenterType.GlobalTx.toInteger()));
                return ResponseEntity.ok(rv);
            }

            ConfigCenter configCenter = configCenterList.get(0);
            configCenter.setStatus(ConfigCenterStatus.Historical.toInteger());
            if (!configCenterService.updateConfigCenter(configCenter)) {
                rv.setMessage("Failed to start global transaction.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
            } else {
                txleCache.removeDistributedConfigCache(TxleConstants.constructConfigCacheKey(null, null, ConfigCenterType.GlobalTx.toInteger()));
            }
        } catch (Exception e) {
            rv.setMessage("Failed to start global transaction.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/findDataDictionaryByKey/{dataDictKey}")
    public ResponseEntity<ReturnValue> findDataDictionaryByKey(@PathVariable String dataDictKey) {
        ReturnValue rv = new ReturnValue();
        try {
            List<DataDictionaryItem> dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList(dataDictKey);
            if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
                LinkedHashMap<String, String> dataDictionaryNameValue = new LinkedHashMap<>();
                dataDictionaryItemList.forEach(dataDictionaryItem -> dataDictionaryNameValue.put(dataDictionaryItem.getName(), dataDictionaryItem.getValue()));
                rv.setData(dataDictionaryNameValue);
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find Data Dictionary by key [" + dataDictKey + "].");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/findGlobalTxServerNames")
    public ResponseEntity<ReturnValue> findGlobalTxServerNames() {
        ReturnValue rv = new ReturnValue();
        try {
            List<String> serverNames = dataDictionaryService.selectGlobalTxServerNames();
            if (serverNames == null || serverNames.isEmpty()) {
                rv.setMessage("Server Names are empty, key.");
            } else {
                rv.setData(serverNames);
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find server names.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/findGlobalTxServerInstanceIds/{serverName}")
    public ResponseEntity<ReturnValue> findGlobalTxServerInstanceIds(@PathVariable String serverName) {
        ReturnValue rv = new ReturnValue();
        try {
            List<String> serverInstanceIds = dataDictionaryService.selectGlobalTxServerInstanceIds(serverName);
            if (serverInstanceIds == null || serverInstanceIds.isEmpty()) {
                rv.setMessage("Server Instance Ids are empty, key.");
            } else {
                rv.setData(serverInstanceIds);
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find server instance ids.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/findGlobalTxServerCategories/{serverName}/{instanceId:.+}")
    public ResponseEntity<ReturnValue> findGlobalTxServerCategories(@PathVariable String serverName, @PathVariable String instanceId) {
        ReturnValue rv = new ReturnValue();
        try {
            List<String> serverCategories = dataDictionaryService.selectGlobalTxServerCategories(serverName, instanceId);
            if (serverCategories == null || serverCategories.isEmpty()) {
                rv.setMessage("Server Categories are empty, key.");
            } else {
                rv.setData(serverCategories);
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find server categories.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping(value = "/accidents/{pageIndex}/{pageSize}/{orderName}/{direction}")
    public ResponseEntity<ReturnValue> findAccidentList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String orderName, @PathVariable String direction) {
        return findAccidentList(pageIndex, pageSize, orderName, direction, "");
    }

    @GetMapping(value = "/accidents/{pageIndex}/{pageSize}/{orderName}/{direction}/{searchText}")
    public ResponseEntity<ReturnValue> findAccidentList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String orderName, @PathVariable String direction, @PathVariable String searchText) {
        ReturnValue rv = new ReturnValue();
        try {
            List<Map<String, Object>> accidentList = accidentHandlingService.findAccidentList(--pageIndex, pageSize, orderName, direction, searchText);
            if (accidentList != null && !accidentList.isEmpty()) {
                rv.setData(JSONObject.parseArray(JSON.toJSONString(accidentList, SerializerFeature.WriteMapNullValue)));
                rv.setTotal(accidentHandlingService.findAccidentCount(searchText));
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find the default list of Accident Handling.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping(value = "/configs/{pageIndex}/{pageSize}/{orderName}/{direction}")
    public ResponseEntity<ReturnValue> findConfigList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String orderName, @PathVariable String direction) {
        return findConfigList(pageIndex, pageSize, orderName, direction, "");
    }

    @GetMapping(value = "/configs/{pageIndex}/{pageSize}/{orderName}/{direction}/{searchText}")
    public ResponseEntity<ReturnValue> findConfigList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String orderName, @PathVariable String direction, @PathVariable String searchText) {
        ReturnValue rv = new ReturnValue();
        try {
            List<Map<String, Object>> configList = configCenterService.findConfigList(--pageIndex, pageSize, orderName, direction, searchText);
            if (configList != null && !configList.isEmpty()) {
                rv.setData(JSONObject.parseArray(JSON.toJSONString(configList, SerializerFeature.WriteMapNullValue)));
                rv.setTotal(configCenterService.findConfigCount(searchText));
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find the default list of Config Center.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @PostMapping(value = "/config")
    public ResponseEntity<ReturnValue> addConfig(@RequestBody ConfigCenter config) {
        ReturnValue rv = new ReturnValue();
        try {
            rv.setData(false);
            if (configCenterService.createConfigCenter(config)) {
                rv.setData(true);
            } else {
                rv.setMessage("Failed to insert Config Center. [" + new GsonBuilder().create().toJson(config) + "]");
            }
        } catch (Exception e) {
            rv.setMessage("Failed to insert Config Center. [" + new GsonBuilder().create().toJson(config) + "]");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @PutMapping(value = "/config")
    public ResponseEntity<ReturnValue> updateConfig(@RequestBody ConfigCenter config) {
        ReturnValue rv = new ReturnValue();
        try {
            rv.setData(false);
            if (configCenterService.updateConfigCenter(config)) {
                rv.setData(true);
            } else {
                rv.setMessage("Failed to update Config Center. [" + new GsonBuilder().create().toJson(config) + "]");
            }
        } catch (Exception e) {
            rv.setMessage("Failed to update Config Center. [" + new GsonBuilder().create().toJson(config) + "]");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @DeleteMapping(value = "/config")
    public ResponseEntity<ReturnValue> deleteConfig(@RequestBody JSONObject jsonConfigIds) {
        ReturnValue rv = new ReturnValue();
        try {
            rv.setData(false);
            if (jsonConfigIds == null) {
                rv.setMessage("The identifications of Config Center are empty.");
                return ResponseEntity.badRequest().body(rv);
            } else {
                AtomicInteger result = new AtomicInteger();
                StringBuilder failedIds = new StringBuilder();
                String configIds = jsonConfigIds.getString("configIds");
                if (configIds == null || configIds.trim().length() == 0) {
                    rv.setMessage("The identifications of Config Center are empty.");
                    return ResponseEntity.badRequest().body(rv);
                }
                List<String> configIdList = Arrays.asList(configIds.split(","));
                if (configIdList != null && !configIdList.isEmpty()) {
                    configIdList.forEach(id -> {
                        long configId = -1;
                        try {
                            configId = Long.parseLong(id.trim());
                            if (configCenterService.deleteConfigCenter(configId)) {
                                result.incrementAndGet();
                            }
                        } catch (Exception e) {
                            ConfigCenter config = null;
                            if (configId > -1) {
                                if (failedIds.length() == 0) {
                                    failedIds.append(configId);
                                } else {
                                    failedIds.append("," + configId);
                                }
                                config = configCenterService.findOne(configId);
                            }
                            LOG.error("Failed to delete config [{}].", config == null ? id : configId, e);
                        }
                    });
                    if (result.get() == 0) {
                        rv.setMessage("Failed to delete configs [" + jsonConfigIds + "].");
                    } else if (result.get() < configIdList.size()) {
                        rv.setMessage("Failed to delete some configs [" + failedIds + "].");
                    } else {
                        rv.setData(true);
                    }
                }
            }
        } catch (Exception e) {
            rv.setMessage("Failed to delete configs [" + jsonConfigIds + "].");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.badRequest().body(rv);
        }
        return ResponseEntity.ok(rv);
    }
}
