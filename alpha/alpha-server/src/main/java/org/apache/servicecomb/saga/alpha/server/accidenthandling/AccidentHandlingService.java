/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.accidenthandling;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.alpha.core.TxleMetrics;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandleStatus;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandleType;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandling;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AccidentHandlingService implements IAccidentHandlingService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final PageRequest PAGEREQUEST = new PageRequest(0, 100);
    private final String accidentPlatformAddress;
    private final int retries;
    // default is 1s
    private final RestTemplate restTemplate;
    private final int interval;

    @Autowired
    private TxEventRepository eventRepository;

    @Autowired
    private TxleMetrics txleMetrics;

    @Autowired
    private IDataDictionaryService dataDictionaryService;

    private AccidentHandlingEntityRepository accidentHandlingEntityRepository;

    public AccidentHandlingService(AccidentHandlingEntityRepository accidentHandlingEntityRepository, String accidentPlatformAddress, int retries, int interval, RestTemplate restTemplate) {
        this.accidentHandlingEntityRepository = accidentHandlingEntityRepository;
        this.accidentPlatformAddress = accidentPlatformAddress;
        this.retries = retries < 0 ? 0 : retries;
        this.interval = interval < 1 ? 1 : interval;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean save(AccidentHandling accidentHandling) {
        try {
            AccidentHandling savedAccident = accidentHandlingEntityRepository.save(accidentHandling);
            if (savedAccident != null) {
                // 设置保存后的id
                accidentHandling.setId(savedAccident.getId());
            }
            return true;
        } catch (Exception e) {
            LOG.error("Failed to save accident handling.", e);
        }
        return false;
    }

    @Override
    public List<AccidentHandling> findAccidentHandlingList() {
        return accidentHandlingEntityRepository.findAccidentHandlingList(PAGEREQUEST);
    }

    @Override
    public List<AccidentHandling> findAccidentHandlingList(AccidentHandleStatus status) {
        return accidentHandlingEntityRepository.findAccidentListByStatus(status.toInteger());
    }

    @Override
    public boolean updateAccidentStatusByIdList(List<Long> idList, AccidentHandleStatus status) {
        return accidentHandlingEntityRepository.updateAccidentStatusByIdList(idList, status.toInteger()) > 0;
    }

    @Override
    public boolean reportMsgToAccidentPlatform(String jsonParams) {
        long a = System.currentTimeMillis();
        LOG.debug(TxleConstants.logDebugPrefixWithTime() + "Message [[{}]] will send to Accident Platform [" + this.accidentPlatformAddress + "].", jsonParams);
        AtomicBoolean result = new AtomicBoolean();
        AtomicInteger invokeTimes = new AtomicInteger();

        try {
            AccidentHandling savedAccident = parseAccidentJson(jsonParams);
            // To save accident to db.
            saveAccidentHandling(savedAccident);

            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleWithFixedDelay(() -> {
                if (result.get()) {
                    accidentHandlingEntityRepository.updateAccidentStatusByIdList(Arrays.asList(savedAccident.getId()), AccidentHandleStatus.SEND_OK.toInteger());
                    scheduler.shutdownNow();
                    // TODO prometheus内部方法持续循环，无法该行代码后的代码
                    txleMetrics.countSuccessfulNumber();
                } else if (invokeTimes.incrementAndGet() > 1 + this.retries) {
                    accidentHandlingEntityRepository.updateAccidentStatusByIdList(Arrays.asList(savedAccident.getId()), AccidentHandleStatus.SEND_FAIL.toInteger());
                    LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to report msg to Accident Platform.");
                    scheduler.shutdownNow();
                    txleMetrics.countFailedNumber();
                } else {
                    // To report accident to Accident Platform.
                    result.set(reportTask(jsonParams));
                }
                // 下一次执行会在本次任务完全执行后的interval时间后执行，如本次任务花费5s，interval为1s，那么将在6s后执行下次任务，而非1s后执行
            }, 0, interval, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to report msg to Accident Platform.");
            txleMetrics.countFailedNumber();
        }
        LOG.info("Method 'AccidentPlatformService.reportMsgToAccidentPlatform' took {} milliseconds.", System.currentTimeMillis() - a);

        return result.get();
    }

    @Override
    public List<Map<String, Object>> findAccidentList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
        List<AccidentHandling> accidentList = this.searchAccidentList(pageIndex, pageSize, orderName, direction, searchText);
        if (accidentList != null && !accidentList.isEmpty()) {
            List<Map<String, Object>> resultAccidentList = new LinkedList<>();

            Map<String, String> typeValueName = new HashMap<>();
            List<DataDictionaryItem> dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList("accident-handle-type");
            if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
                dataDictionaryItemList.forEach(dd -> typeValueName.put(dd.getValue(), dd.getName()));
            }

            Map<String, String> statusValueName = new HashMap<>();
            dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList("accident-handle-status");
            if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
                dataDictionaryItemList.forEach(dd -> statusValueName.put(dd.getValue(), dd.getName()));
            }

            accidentList.forEach(accident -> resultAccidentList.add(accident.toMap(typeValueName.get(String.valueOf(accident.getType())), statusValueName.get(String.valueOf(accident.getStatus())))));

            return resultAccidentList;
        }
        return null;
    }

    private List<AccidentHandling> searchAccidentList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
        try {
            pageIndex = pageIndex < 1 ? 0 : pageIndex;
            pageSize = pageSize < 1 ? 100 : pageSize;

            Sort.Direction sd = Sort.Direction.DESC;
            if (orderName == null || orderName.length() == 0) {
                orderName = "completetime";
            }
            if ("asc".equalsIgnoreCase(direction)) {
                sd = Sort.Direction.ASC;
            }

            PageRequest pageRequest = new PageRequest(pageIndex, pageSize, sd, orderName);
            if (searchText == null || searchText.length() == 0) {
                return accidentHandlingEntityRepository.findAccidentList(pageRequest);
            }
            return accidentHandlingEntityRepository.findAccidentList(pageRequest, searchText);
        } catch (Exception e) {
            LOG.error("Failed to find the list of Accident Handling. params {pageIndex: [{}], pageSize: [{}], orderName: [{}], direction: [{}], searchText: [{}]}.", pageIndex, pageSize, orderName, direction, searchText, e);
        }
        return null;
    }

    @Override
    public long findAccidentCount(String searchText) {
        if (searchText == null || searchText.length() == 0) {
            return accidentHandlingEntityRepository.findAccidentCount();
        }
        return accidentHandlingEntityRepository.findAccidentCount(searchText);
    }

    private AccidentHandling parseAccidentJson(String jsonParams) {
        JsonObject jsonObject = new JsonParser().parse(jsonParams).getAsJsonObject();
        String serviceName = "", instanceId = "", globalTxId = "", localTxId = "", bizinfo = "", remark = "";
        int type = 1;
        JsonElement jsonElement = jsonObject.get("servicename");
        if (jsonElement != null) {
            serviceName = jsonElement.getAsString();
        }
        jsonElement = jsonObject.get("instanceid");
        if (jsonElement != null) {
            instanceId = jsonElement.getAsString();
        }
        jsonElement = jsonObject.get("globaltxid");
        if (jsonElement != null) {
            globalTxId = jsonElement.getAsString();
        }
        jsonElement = jsonObject.get("localtxid");
        if (jsonElement != null) {
            localTxId = jsonElement.getAsString();
        }
        jsonElement = jsonObject.get("type");
        if (jsonElement != null) {
            type = jsonElement.getAsInt();
        }
        jsonElement = jsonObject.get("bizinfo");
        if (jsonElement != null) {
            bizinfo = jsonElement.getAsString();
        }
        jsonElement = jsonObject.get("remark");
        if (jsonElement != null) {
            remark = jsonElement.getAsString();
        }
        return new AccidentHandling(serviceName, instanceId, globalTxId, localTxId, AccidentHandleType.convertTypeFromValue(type), bizinfo, remark);
    }

    private boolean saveAccidentHandling(AccidentHandling accident) {
        boolean result = false;
        try {
            if (accident.getServicename() == null || accident.getInstanceid() == null || "".equals(accident.getServicename()) || "".equals(accident.getInstanceid())) {
                Optional<TxEvent> event = eventRepository.findTxStartedEvent(accident.getGlobaltxid(), accident.getLocaltxid());
                if (event != null && event.get() != null) {
                    accident.setServicename(event.get().serviceName());
                    accident.setInstanceid(event.get().instanceId());
                }
            }
            result = accidentHandlingEntityRepository.save(accident) != null;
        } catch (Exception e) {
            // That's not too important for main business to throw an exception.
            LOG.error("Failed to save accident to db, accident [{}].", accident, e);
        } finally {
            LOG.error("Saved accident to db, result [{}] and accident [{}].", result, accident);
        }
        return result;
    }

    private boolean reportTask(String jsonParams) {
        boolean result = false;
        try {
            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(mediaType);
            HttpEntity<String> entity = new HttpEntity<>(jsonParams, headers);
            String reportResponse = restTemplate.postForObject(this.accidentPlatformAddress, entity, String.class);
            result = TxleConstants.OK.equals(reportResponse);
        } catch (Exception e) {
            LOG.error("Failed to report msg [{}] to Accident Platform [{}].", jsonParams, this.accidentPlatformAddress, e);
        } finally {
            LOG.error("Reported accident to platform, result [{}], platform address [{}] and accident [{}].", result, this.accidentPlatformAddress, jsonParams);
        }
        return result;
    }
}
