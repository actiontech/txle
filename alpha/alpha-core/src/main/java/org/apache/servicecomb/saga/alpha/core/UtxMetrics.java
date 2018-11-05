package org.apache.servicecomb.saga.alpha.core;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;

/**
 * Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
 * @author Gannalyo
 * @date 20181024
 */
public class UtxMetrics extends Collector {

    private static final Logger LOG = LoggerFactory.getLogger(UtxMetrics.class);

    @Autowired
    private TxEventRepository eventRepository;

    // TODO The value of 'Counter' will become zero after restarting current application.
    // ps: Support cluster mode, in the cluster cases, to distinguish every instance by labelNames. Please view the prometheus.yml
    // Such as: utx_transaction_total{instance=~"utx8099",job=~"utx"} or utx_transaction_total{instance=~"utx8099",job=~"utx"}, summary is: sum(utx_transaction_total{job=~"utx"})
    public static final Counter UTX_TRANSACTION_TOTAL = buildCounter("utx_transaction_total", "The total number of transactions.");
    public static final Counter UTX_TRANSACTION_FAILED_TOTAL = buildCounter("utx_transaction_failed_total", "The total number of retried transactions.");
    public static final Counter UTX_TRANSACTION_ROLLBACKED_TOTAL = buildCounter("utx_transaction_rollbacked_total", "The total number of rollbacked transactions.");
    public static final Counter UTX_TRANSACTION_RETRIED_TOTAL = buildCounter("utx_transaction_retried_total", "The total number of retried transactions..");
    public static final Counter UTX_TRANSACTION_TIMEOUT_TOTAL = buildCounter("utx_transaction_timeout_total", "The total number of timeout transactions.");
    public static final Counter UTX_TRANSACTION_PAUSED_TOTAL = buildCounter("utx_transaction_paused_total", "The total number of paused transactions.");
    public static final Counter UTX_TRANSACTION_CONTINUED_TOTAL = buildCounter("utx_transaction_continued_total", "The total number of continued transactions.");
    public static final Counter UTX_TRANSACTION_AUTOCONTINUED_TOTAL = buildCounter("utx_transaction_autocontinued_total", "The total number of auto-continued transactions.");

    // To store 'globalTxId' for aborted events, it is the aim to avoid counting repeat event number. Do not need to pay more attention on restart, cluster and concurrence.
    public static final Map<String, Set<String>> globalTxIdAndTypes = new HashMap<>();

    private static Counter buildCounter(String name, String help) {
        return Counter.build(name, help)/*.labelNames("instance", "job")*/.register();
    }

    public static void countTxNumber(TxEvent event, boolean isTimeout, boolean isRetried) {
        try {
            Set<String> eventTypesOfCurrentTx = globalTxIdAndTypes.get(event.globalTxId());
            if (eventTypesOfCurrentTx == null) {
                eventTypesOfCurrentTx = new HashSet<>();
            }
            String type = event.type();
            if (!eventTypesOfCurrentTx.contains(type)) {
                eventTypesOfCurrentTx.add(type);
                globalTxIdAndTypes.put(event.globalTxId(), eventTypesOfCurrentTx);

                if (SagaEndedEvent.name().equals(type)) {
                    UTX_TRANSACTION_TOTAL.inc();
                    globalTxIdAndTypes.remove(event.globalTxId());
                    return;
                } else if (TxAbortedEvent.name().equals(type)) {
                    UTX_TRANSACTION_FAILED_TOTAL.inc();
                } else if (TxCompensatedEvent.name().equals(type)) {
                    UTX_TRANSACTION_ROLLBACKED_TOTAL.inc();
                } else if (AdditionalEventType.SagaPausedEvent.name().equals(type)) {
                    UTX_TRANSACTION_PAUSED_TOTAL.inc();
                    return;
                } else if (AdditionalEventType.SagaContinuedEvent.name().equals(type)) {
                    UTX_TRANSACTION_CONTINUED_TOTAL.inc();
                    return;
                } else if (AdditionalEventType.SagaAutoContinuedEvent.name().equals(type)) {
                    UTX_TRANSACTION_AUTOCONTINUED_TOTAL.inc();
                    return;
                }
            }

            // handle retried transaction
            if (isTimeout && !eventTypesOfCurrentTx.contains("TxTimeoutEvent")) {
                eventTypesOfCurrentTx.add("TxTimeoutEvent");
                globalTxIdAndTypes.put(event.globalTxId(), eventTypesOfCurrentTx);
                UTX_TRANSACTION_TIMEOUT_TOTAL.inc();
            }

            // handle retried transaction. ps: do not support retries in timeout case.
            if (isRetried && !eventTypesOfCurrentTx.contains("TxRetriedEvent")) {
                eventTypesOfCurrentTx.add("TxRetriedEvent");
                globalTxIdAndTypes.put(event.globalTxId(), eventTypesOfCurrentTx);
                UTX_TRANSACTION_RETRIED_TOTAL.inc();
            }
        } catch (Exception e) {
            LOG.error("Count utx transaction number exception: " + e);
        }
    }

    public UtxMetrics(TxEventRepository eventRepository) {
        if (this.eventRepository == null) {
            this.eventRepository = eventRepository;
        }

        try {
//            DefaultExports.initialize();
            new HTTPServer(8099);// TODO 端口配置？？？
            this.register();
        } catch (IOException e) {
            LOG.error("Initialize utx metrics server exception: " + e);
        }

    }

    // Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
    @Override
    public List<MetricFamilySamples> collect() {
        System.out.println("Fetch UTX Metrics -  " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()));
        List<MetricFamilySamples> metricList = new ArrayList<>();

        // expose the 'utx_transaction_total' metric.
//        metricList.add(new GaugeMetricFamily("utx_transaction_total", "The total number of transactions", eventRepository.totalTransaction()));
//        metricList.add(new GaugeMetricFamily("utx_transaction_failed_total", "The total number of failed transactions", eventRepository.totalFailedTransaction()));
//        metricList.add(new GaugeMetricFamily("utx_transaction_rollbacked_total", "The total number of rollbacked transactions", eventRepository.totalRollbackedTransaction()));
//        metricList.add(new GaugeMetricFamily("utx_transaction_retried_total", "The total number of retried transactions", eventRepository.totalRetriedTransaction()));
//        metricList.add(new GaugeMetricFamily("utx_transaction_timeout_total", "The total number of timeout transactions", eventRepository.totalTimeoutTransaction()));

        return metricList;
    }

}
