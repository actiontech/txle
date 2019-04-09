package org.apache.servicecomb.saga.common.rmi.accidentplatform;

import org.apache.servicecomb.saga.common.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AccidentPlatformService implements IAccidentPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(AccidentPlatformService.class);
    private final String accidentPlatformAddress;
    private final int retries;
    private final int interval;// default is 1s
    private final RestTemplate restTemplate;

    public AccidentPlatformService(String accidentPlatformAddress, int retries, int interval, RestTemplate restTemplate) {
        this.accidentPlatformAddress = accidentPlatformAddress;
        this.retries = retries < 0 ? 0 : retries;
        this.interval = interval < 1 ? 1 : interval;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean reportMsgToAccidentPlatform(String jsonParams) {
        LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Message [[{}]] will send to Accident Platform [" + this.accidentPlatformAddress + "].", jsonParams);
        AtomicBoolean result = new AtomicBoolean();
        AtomicInteger invokeTimes = new AtomicInteger();

        try {
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleWithFixedDelay(() -> {
                if (result.get()) {
                    UtxAccidentMetrics.countSuccessfulNumber();
                    scheduler.shutdownNow();
                } else if (invokeTimes.incrementAndGet() > 1 + this.retries) {
                    UtxAccidentMetrics.countFailedNumber();
                    scheduler.shutdownNow();
                    LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Failed to report msg to Accident Platform.");
                } else {
                    result.set(reportTask(jsonParams));
                }
                // 下一次执行会在本次任务完全执行后的interval时间后执行，如本次任务花费5s，interval为1s，那么将在6s后执行下次任务，而非1s后执行
            }, 0, interval, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Failed to report msg to Accident Platform.");
            UtxAccidentMetrics.countFailedNumber();
        }

        return result.get();
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
            LOG.error("Failed to report msg to Accident Platform.", e);
        }
        return result;
    }

}