package org.apache.servicecomb.saga.alpha.server.accidenthandling;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.*;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AccidentHandlingService implements IAccidentHandlingService {
    private static final Logger LOG = LoggerFactory.getLogger(AccidentHandlingService.class);
    private final String accidentPlatformAddress;
    private final int retries;
    private final int interval;// default is 1s
    private final RestTemplate restTemplate;

    @Autowired
    IConfigCenterService dbDegradationConfigService;

    @Autowired
    TxEventRepository eventRepository;

    @Autowired
    IAccidentHandlingRepository accidentHandlingRepository;

    public AccidentHandlingService(String accidentPlatformAddress, int retries, int interval, RestTemplate restTemplate) {
        this.accidentPlatformAddress = accidentPlatformAddress;
        this.retries = retries < 0 ? 0 : retries;
        this.interval = interval < 1 ? 1 : interval;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean reportMsgToAccidentPlatform(String jsonParams) {
        long a = System.currentTimeMillis();
        LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Message [[{}]] will send to Accident Platform [" + this.accidentPlatformAddress + "].", jsonParams);
        AtomicBoolean result = new AtomicBoolean();
        AtomicInteger invokeTimes = new AtomicInteger();

        try {
            AccidentHandling savedAccident = parseAccidentJson(jsonParams);
            // To save accident to db.
            saveAccidentHandling(savedAccident);

            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleWithFixedDelay(() -> {
                if (result.get()) {
                    scheduler.shutdownNow();
                    UtxAccidentMetrics.countSuccessfulNumber();
                    accidentHandlingRepository.updateAccidentStatusByIdList(Arrays.asList(savedAccident.getId()), AccidentHandleStatus.SEND_OK);
                } else if (invokeTimes.incrementAndGet() > 1 + this.retries) {
                    scheduler.shutdownNow();
                    UtxAccidentMetrics.countFailedNumber();
                    accidentHandlingRepository.updateAccidentStatusByIdList(Arrays.asList(savedAccident.getId()), AccidentHandleStatus.SEND_FAIL);
                    LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Failed to report msg to Accident Platform.");
                } else {
                    // To report accident to Accident Platform.
                    result.set(reportTask(jsonParams));
                }
                // 下一次执行会在本次任务完全执行后的interval时间后执行，如本次任务花费5s，interval为1s，那么将在6s后执行下次任务，而非1s后执行
            }, 0, interval, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Failed to report msg to Accident Platform.");
            UtxAccidentMetrics.countFailedNumber();
        }
        LOG.info("Method 'AccidentPlatformService.reportMsgToAccidentPlatform' took {} milliseconds.", System.currentTimeMillis() - a);

        return result.get();
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
        try {
            if (accident.getServicename() == null || accident.getInstanceid() == null || "".equals(accident.getServicename()) || "".equals(accident.getInstanceid())) {
                Optional<TxEvent> event = eventRepository.findTxStartedEvent(accident.getGlobaltxid(), accident.getLocaltxid());
                if (event != null && event.get() != null) {
                    accident.setServicename(event.get().serviceName());
                    accident.setInstanceid(event.get().instanceId());
                }
            }
            return accidentHandlingRepository.save(accident);
        } catch (Exception e) {// That's not too important for main business to throw an exception.
            LOG.error("Failed to save accident to db, accident [{}].", accident, e);
        }
        return false;
    }

    private boolean reportTask(String jsonParams) {
        boolean result = false;
        try {
            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = MediaType.parseMediaType("application/json; charset=UTF-8");
            headers.setContentType(mediaType);
            HttpEntity<String> entity = new HttpEntity<>(jsonParams, headers);
            String reportResponse = restTemplate.postForObject(this.accidentPlatformAddress, entity, String.class);
            result = UtxConstants.OK.equals(reportResponse);
        } catch (Exception e) {
            LOG.error("Failed to report msg [{}] to Accident Platform [{}].", jsonParams, this.accidentPlatformAddress, e);
        }
        return result;
    }

}