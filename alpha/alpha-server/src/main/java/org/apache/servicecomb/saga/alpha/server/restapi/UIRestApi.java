package org.apache.servicecomb.saga.alpha.server.restapi;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.servicecomb.saga.alpha.core.*;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.alpha.server.TableFieldRepository;
import org.apache.servicecomb.saga.alpha.server.configcenter.DBDegradationConfigService;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.common.ReturnValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.servicecomb.saga.common.EventType.*;

@RestController
public class UIRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(UIRestApi.class);

    @Autowired
    private HttpServletRequest request;

    private TableFieldRepository tableFieldRepository;

    private TxEventRepository eventRepository;

    @Autowired
    private DBDegradationConfigService dbDegradationConfigService;

    @Autowired
    UtxMetrics utxMetrics;

    private final Gson gson = new GsonBuilder().create();

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
                rv.setData(gson.toJson(tableFieldEntityList));
            }
        } catch (Exception e) {
            rv.setMessage("Failed to get columns from " + tableDesc + ".");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }

        return ResponseEntity.ok(rv);
    }

    @GetMapping(value = "/globalTransactions")
    public ResponseEntity<ReturnValue> findTxList() {
        return findTxList(0, 100, "", "", "");
    }

    @GetMapping(value = "/globalTransactions/{pageIndex}/{pageSize}/{searchText}")
    public ResponseEntity<ReturnValue> findTxList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String searchText) {
        if (searchText != null) searchText = searchText.trim();
        return findTxList(pageIndex, pageSize, "", "", searchText);
    }

    @GetMapping(value = "/globalTransactions/{pageIndex}/{pageSize}/{orderName}/{direction}/{searchText}")
    public ResponseEntity<ReturnValue> findTxList(@PathVariable int pageIndex, @PathVariable int pageSize, @PathVariable String orderName, @PathVariable String direction, @PathVariable String searchText) {
        ReturnValue rv = new ReturnValue();
        try {
            // 确定本次分页查询的全局事务
            List<TxEvent> txStartedEventList = eventRepository.findTxList(pageIndex, pageSize, orderName, direction, searchText);
            if (txStartedEventList != null && !txStartedEventList.isEmpty()) {
                List<Map<String, Object>> resultTxEventList = new LinkedList<>();

                List<String> globalTxIdList = new ArrayList<>();
                txStartedEventList.forEach(event -> {
                    globalTxIdList.add(event.globalTxId());
                    resultTxEventList.add(event.toMap());
                });

                List<TxEvent> txEventList = eventRepository.selectTxEventByGlobalTxIds(globalTxIdList);
                if (txEventList != null && !txEventList.isEmpty()) {
                    // 计算全局事务的状态
                    computeGlobalTxStatus(txEventList, resultTxEventList);
                }

                rv.setData(gson.toJson(resultTxEventList));
                rv.setTotal(eventRepository.findTxListCount(searchText));
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find the default list of Global Transaction.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    // 计算全局事务的状态
    private void computeGlobalTxStatus(List<TxEvent> txEventList, List<Map<String, Object>> resultTxEventList) {
        // 0-运行中，1-运行异常，2-暂停，3-正常结束，4-异常结束
        resultTxEventList.forEach(txMap -> txMap.put("status", 0));

        txEventList.forEach(event -> {
            if (TxAbortedEvent.name().equals(event.type())) {
                for (Map<String, Object> txMap : resultTxEventList) {
                    if (event.globalTxId().equals(txMap.get("globalTxId").toString())) {
                        txMap.put("status", 1);// 异常状态
                        break;
                    }
                }
            }
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        txEventList.forEach(event -> {
            if (SagaEndedEvent.name().equals(event.type())) {
                for (Map<String, Object> txMap : resultTxEventList) {
                    if (event.globalTxId().equals(txMap.get("globalTxId").toString())) {
                        txMap.put("endTime", sdf.format(event.creationTime()));// ****设置结束时间****
                        if (Integer.parseInt(txMap.get("status").toString()) == 0) {
                            txMap.put("status", 3);// 正常结束
                        } else {
                            txMap.put("status", 4);// 异常结束
                        }
                        break;
                    }
                }
            }
        });

        resultTxEventList.forEach(txMap -> {
            if (Integer.parseInt(txMap.get("status").toString()) == 0) {// 正常状态场景才去验证是否暂停
                txEventList.forEach(event -> {
                    if (event.globalTxId().equals(txMap.get("globalTxId").toString()) && (AdditionalEventType.SagaPausedEvent.name().equals(event.type()) || AdditionalEventType.SagaAutoContinuedEvent.name().equals(event.type()))) {
                        List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                        if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                            if (pauseContinueEventList.size() % 2 == 1) {// 暂停状态
                                txMap.put("status", 2);// 暂停
                            }
                        }
                    }
                });
            }
        });
    }

    @PostMapping(value = "/subTransactions")
    public ResponseEntity<ReturnValue> findSubTxList(@RequestBody String globalTxIds) {
        ReturnValue rv = new ReturnValue();
        try {
            if (globalTxIds != null && globalTxIds.length() > 0) {
                List<String> globalTxIdList = Arrays.asList(globalTxIds.split(","));
                List<TxEvent> txEventList = eventRepository.selectSpecialColumnsOfTxEventByGlobalTxIds(globalTxIdList);
                if (txEventList != null && !txEventList.isEmpty()) {
                    List<Map<String, Object>> resultTxEventList = new LinkedList<>();
                    txEventList.forEach(event -> {
                        if (TxStartedEvent.name().equals(event.type())) {
                            resultTxEventList.add(event.toMap());
                        }
                    });

                    computeSubTxStatus(txEventList, resultTxEventList);

                    rv.setData(gson.toJson(resultTxEventList));
                }
            }
        } catch (Exception e) {
            rv.setMessage("Failed to find the default list of Sub Transaction.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    // 计算子事务的状态
    private void computeSubTxStatus(List<TxEvent> txEventList, List<Map<String, Object>> resultTxEventList) {
        // 0-运行中，1-运行异常，2-暂停，3-正常结束，4-异常结束
        resultTxEventList.forEach(txMap -> txMap.put("status", 0));

        txEventList.forEach(event -> {
            if (TxAbortedEvent.name().equals(event.type())) {
                for (Map<String, Object> txMap : resultTxEventList) {
                    if (event.localTxId().equals(txMap.get("localTxId").toString())) {
                        txMap.put("status", 1);// 异常状态
                        break;
                    }
                }
            }
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        txEventList.forEach(event -> {
            if (TxEndedEvent.name().equals(event.type())) {
                for (Map<String, Object> txMap : resultTxEventList) {
                    if (event.localTxId().equals(txMap.get("localTxId").toString())) {
                        txMap.put("endTime", sdf.format(event.creationTime()));// ****设置结束时间****
                        if (Integer.parseInt(txMap.get("status").toString()) == 0) {
                            txMap.put("status", 3);// 正常结束
                        } else {
                            txMap.put("status", 4);// 异常结束
                        }
                        break;
                    }
                }
            }
        });

        resultTxEventList.forEach(txMap -> {
            if (Integer.parseInt(txMap.get("status").toString()) == 0) {// 正常状态场景才去验证是否暂停
                txEventList.forEach(event -> {
                    if (event.localTxId().equals(txMap.get("localTxId").toString()) && (AdditionalEventType.SagaPausedEvent.name().equals(event.type()) || AdditionalEventType.SagaAutoContinuedEvent.name().equals(event.type()))) {
                        List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                        if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                            if (pauseContinueEventList.size() % 2 == 1) {// 暂停状态
                                txMap.put("status", 2);// 暂停
                            }
                        }
                    }
                });
            }
        });
    }

    @PostMapping("/pauseGlobalTransactions")
    public ResponseEntity<ReturnValue> pauseGlobalTransactions(@RequestBody JSONObject jsonParams) {
        ReturnValue rv = new ReturnValue();
        if (jsonParams == null) {
            rv.setMessage("The identifications of Global Transactions are empty.");
            return ResponseEntity.badRequest().body(rv);
        }
        String globalTxIds = jsonParams.getString("globalTxIds");
        return saveOperationTxEventWithVerification(globalTxIds, jsonParams.getIntValue("pausePeriod"), "pause");
    }

    @PostMapping("/recoverGlobalTransactions")
    public ResponseEntity<ReturnValue> recoverGlobalTransactions(@RequestBody String globalTxIds) {
        return saveOperationTxEventWithVerification(globalTxIds, 0, "recover");
    }

    @PostMapping("/terminateGlobalTransactions")
    public ResponseEntity<ReturnValue> terminateGlobalTransactions(@RequestBody String globalTxIds) {
        return saveOperationTxEventWithVerification(globalTxIds, 0, "terminate");
    }

    private ResponseEntity<ReturnValue> saveOperationTxEventWithVerification(String globalTxIds, int pausePeriod, String operation) {
        ReturnValue rv = new ReturnValue();
        try {
            if (globalTxIds == null || globalTxIds.trim().length() == 0) {
                rv.setMessage("The identifications of Global Transactions are empty.");
                return ResponseEntity.badRequest().body(rv);
            }

            List<String> globalTxIdList = new ArrayList<>();// Arrays.asList(globalTxIds.split(","));// 这样写，后续将不能执行remove方法
            for (String globalTxId : globalTxIds.split(",")) {
                globalTxIdList.add(globalTxId);
            }

            // To filter which have been over.
            List<TxEvent> txEventList = eventRepository.selectTxEventByGlobalTxIds(globalTxIdList);
            if (txEventList != null && !txEventList.isEmpty()) {
                AtomicReference<String> operationAdjective = new AtomicReference<>("pause".equals(operation) ? "suspended" : "recover".equals(operation) ? "normal" : "terminated");
                txEventList.forEach(event -> {
                    if (SagaEndedEvent.name().equals(event.type())) {
                        globalTxIdList.remove(event.globalTxId());// 移除已结束的
                    } else {
                        List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                        if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                            if ("pause".equals(operation) && pauseContinueEventList.size() % 2 == 1) {// 移除暂停的
                                globalTxIdList.remove(event.globalTxId());
                            } else if ("recover".equals(operation) && pauseContinueEventList.size() % 2 == 0) {// 移除非暂停的
                                globalTxIdList.remove(event.globalTxId());
                            }
                        }
                    }
                });
                if (globalTxIdList.isEmpty()) {
                    rv.setMessage("All global transactions have been over or " + operationAdjective.get() + ".");
                    return ResponseEntity.ok(rv);
                }
            }

            txEventList.forEach(event -> {
                if (globalTxIdList.contains(event.globalTxId())) {
                    String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
                    String typeName = AdditionalEventType.SagaPausedEvent.name();
                    if ("recover".equals(operation)) {
                        typeName = AdditionalEventType.SagaContinuedEvent.name();
                    } else if ("terminate".equals(operation)) {
                        typeName = EventType.TxAbortedEvent.name();
                    }
                    TxEvent pausedEvent = new TxEvent(ip_port, ip_port, event.globalTxId(), event.localTxId(), event.parentTxId(), typeName, "", pausePeriod, "", 0, event.category(), null);
                    eventRepository.save(pausedEvent);
                    if ("terminate".equals(operation)) {
                        eventRepository.save(new TxEvent(event.serviceName(), event.instanceId(), event.globalTxId(), event.globalTxId(), null, SagaEndedEvent.name(), "", event.category(), null));
                    }
                    utxMetrics.countTxNumber(event, false, false);
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
            // 检测是否已暂停全局事务
            List<ConfigCenter> configCenterList = dbDegradationConfigService.selectConfigCenterByType(null, ConfigCenterStatus.Normal.toInteger(), ConfigCenterType.PauseGlobalTx.toInteger());
            if (configCenterList != null && !configCenterList.isEmpty()) {
                return ResponseEntity.ok(rv);
            }

            // 1.暂停全局事务配置
            String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
            dbDegradationConfigService.createConfigCenter(new ConfigCenter(null, null, ConfigCenterStatus.Normal, 1, ConfigCenterType.PauseGlobalTx, "enabled", ip_port + " - pauseAllTransaction"));

            // 2.对未结束且未暂停的全局事务逐一设置暂停事件，不会出现某全局事务还未等设置暂停事件就结束的情况，因为上面先生成了暂停配置
            List<TxEvent> unendedTxEventList = eventRepository.selectUnendedTxEvents(EventScanner.getUnendedMinEventId());
            if (unendedTxEventList != null && !unendedTxEventList.isEmpty()) {
                List<String> globalTxIdList = new ArrayList<>();
                unendedTxEventList.forEach(event -> globalTxIdList.add(event.globalTxId()));

                unendedTxEventList.forEach(event -> {
                    List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                    if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                        if (pauseContinueEventList.size() % 2 == 1) {// 移除暂停的
                            globalTxIdList.remove(event.globalTxId());
                        }
                    }
                });
                if (globalTxIdList.isEmpty()) {
                    return ResponseEntity.ok(rv);
                }

                unendedTxEventList.forEach(event -> {
                    if (globalTxIdList.contains(event.globalTxId())) {
                        TxEvent pausedEvent = new TxEvent(ip_port, ip_port, event.globalTxId(), event.localTxId(), event.parentTxId(), AdditionalEventType.SagaPausedEvent.name(), "", 0, "", 0, event.category(), null);
                        eventRepository.save(pausedEvent);
                        utxMetrics.countTxNumber(event, false, false);
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
            // 1.去除暂停全局事务配置
            List<ConfigCenter> configCenterList = dbDegradationConfigService.selectConfigCenterByType(null, ConfigCenterStatus.Normal.toInteger(), ConfigCenterType.PauseGlobalTx.toInteger());
            if (configCenterList != null && !configCenterList.isEmpty()) {
                ConfigCenter configCenter = configCenterList.get(0);
                configCenter.setStatus(ConfigCenterStatus.Historical.toInteger());
                dbDegradationConfigService.updateConfigCenter(configCenter);
                return ResponseEntity.ok(rv);
            }

            // 2.对未结束且未暂停的全局事务逐一设置暂停事件，不会出现某全局事务还未等设置暂停事件就结束的情况，因为上面先生成了暂停配置
            List<TxEvent> unendedTxEventList = eventRepository.selectUnendedTxEvents(EventScanner.getUnendedMinEventId());
            if (unendedTxEventList != null && !unendedTxEventList.isEmpty()) {
                List<String> globalTxIdList = new ArrayList<>();
                unendedTxEventList.forEach(event -> globalTxIdList.add(event.globalTxId()));

                unendedTxEventList.forEach(event -> {
                    List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(event.globalTxId());
                    if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                        if (pauseContinueEventList.size() % 2 == 0) {// 移除非暂停的
                            globalTxIdList.remove(event.globalTxId());
                        }
                    }
                });
                if (globalTxIdList.isEmpty()) {
                    return ResponseEntity.ok(rv);
                }

                String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
                unendedTxEventList.forEach(event -> {
                    if (globalTxIdList.contains(event.globalTxId())) {
                        TxEvent pausedEvent = new TxEvent(ip_port, ip_port, event.globalTxId(), event.localTxId(), event.parentTxId(), AdditionalEventType.SagaContinuedEvent.name(), "", 0, "", 0, event.category(), null);
                        eventRepository.save(pausedEvent);
                        utxMetrics.countTxNumber(event, false, false);
                    }
                });
            }
        } catch (Exception e) {
            rv.setMessage("Failed to recover all global transactions.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

    @GetMapping("/degradeGlobalTransaction")
    public ResponseEntity<ReturnValue> degradeGlobalTransaction() {
        ReturnValue rv = new ReturnValue();
        try {
            boolean enabledTx = dbDegradationConfigService.isEnabledTx(null, ConfigCenterType.GlobalTx);
            if (!enabledTx) {
                rv.setMessage("Sever has been degraded for the Global Transaction.");
                return ResponseEntity.ok(rv);
            }
            String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
            boolean enabled = dbDegradationConfigService.createConfigCenter(new ConfigCenter(null, null, ConfigCenterStatus.Normal, 1, ConfigCenterType.GlobalTx, "disabled", ip_port + " - degradeGlobalTransaction"));
            if (!enabled) {
                rv.setMessage("Failed to save the degradation configuration of global transaction.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
            } else {
                CacheRestApi.clearByKey("null_" + ConfigCenterStatus.Normal.toInteger() + "_" + ConfigCenterType.GlobalTx.toInteger());
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
            List<ConfigCenter> configCenterList = dbDegradationConfigService.selectConfigCenterByType(null, ConfigCenterStatus.Normal.toInteger(), ConfigCenterType.GlobalTx.toInteger());
            if (configCenterList == null || configCenterList.isEmpty()) {
                rv.setMessage("Sever has been started for the Global Transaction.");
                return ResponseEntity.ok(rv);
            }

            ConfigCenter configCenter = configCenterList.get(0);
            configCenter.setStatus(ConfigCenterStatus.Historical.toInteger());
            if (!dbDegradationConfigService.updateConfigCenter(configCenter)) {
                rv.setMessage("Failed to start global transaction.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
            } else {
                CacheRestApi.clearByKey("null_" + ConfigCenterStatus.Normal.toInteger() + "_" + ConfigCenterType.GlobalTx.toInteger());
            }
        } catch (Exception e) {
            rv.setMessage("Failed to start global transaction.");
            LOG.error(rv.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rv);
        }
        return ResponseEntity.ok(rv);
    }

}
