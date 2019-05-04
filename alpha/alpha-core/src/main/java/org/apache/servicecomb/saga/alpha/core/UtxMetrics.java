package org.apache.servicecomb.saga.alpha.core;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.servicecomb.saga.common.EventType.*;

/**
 * Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
 * @author Gannalyo
 * @date 20181024
 */
public class UtxMetrics extends Collector {

    private static final Logger LOG = LoggerFactory.getLogger(UtxMetrics.class);

    @Autowired
    IConfigCenterService dbDegradationConfigService;

    // TODO The value of 'Counter' will become zero after restarting current application.
    // ps: Support cluster mode, in the cluster cases, to distinguish every instance by labelNames. Please view the prometheus.yml
    // Such as: utx_transaction_total{instance=~"utx8099",job=~"utx"} or utx_transaction_total{instance=~"utx8099",job=~"utx"}, summary is: sum(utx_transaction_total{job=~"utx"})
    private static final Counter UTX_TRANSACTION_TOTAL = buildCounter("utx_transaction_total", "Total number of transactions.");
    private static final Counter UTX_TRANSACTION_SUCCESSFUL_TOTAL = buildCounter("utx_transaction_successful_total", "Total number of successful transactions.");
    private static final Counter UTX_TRANSACTION_FAILED_TOTAL = buildCounter("utx_transaction_failed_total", "Total number of transactions which had abnormity occurs.");// 发生异常的数量
    private static final Counter UTX_TRANSACTION_ROLLBACKED_TOTAL = buildCounter("utx_transaction_rollbacked_total", "Total number of rollbacked transactions.");
    private static final Counter UTX_TRANSACTION_RETRIED_TOTAL = buildCounter("utx_transaction_retried_total", "Total number of retried transactions..");
    private static final Counter UTX_TRANSACTION_TIMEOUT_TOTAL = buildCounter("utx_transaction_timeout_total", "Total number of timeout transactions.");
    private static final Counter UTX_TRANSACTION_PAUSED_TOTAL = buildCounter("utx_transaction_paused_total", "Total number of paused transactions.");
    private static final Counter UTX_TRANSACTION_CONTINUED_TOTAL = buildCounter("utx_transaction_continued_total", "Total number of continued transactions.");
    private static final Counter UTX_TRANSACTION_AUTOCONTINUED_TOTAL = buildCounter("utx_transaction_autocontinued_total", "Total number of auto-continued transactions.");

    private static final Counter UTX_TRANSACTION_CHILD_TOTAL = buildCounter("utx_transaction_child_total", "Total number of child transactions.");
    private static final Set<String> localTxIdSet = new HashSet<>();// To support retry situation.

    // mark duration
    // To store 'globalTxId' or 'localTxId' for aborted events, it is the aim to avoid counting repeat event number. Do not need to pay more attention on restart, cluster and concurrence.
    private static final Map<String, Gauge.Timer> txIdAndGaugeTimer = new HashMap<>();
    // Total seconds spent for someone transaction. Ps: It will show one row only if you search this metric in 'http://ip:9090/graph'.
    // But, Prometheus will record every metric in different times, and you can search, like 'utx_transaction_time_seconds_total[5m]', then it will show many rows to you.
    private static final Gauge UTX_TRANSACTION_TIME_SECONDS_TOTAL = buildGauge("utx_transaction_time_seconds_total", "Total seconds spent executing the global transaction.");
    private static final Gauge UTX_TRANSACTION_CHILD_TIME_SECONDS_TOTAL = buildGauge("utx_transaction_child_time_seconds_total", "Total seconds spent executing the child transaction.");

    // To avoid to count repeatedly for the same tx and type.
    private static final Map<String, Set<String>> globalTxIdAndTypes = new HashMap<>();
    private static final String[] labelNames = new String[]{"business", "category"};

    // mark duration, guarantee start and end have the same timer. Do not use txIdAndGaugeTimer, because the globalTxId is able to be null.
    private static final ThreadLocal<Gauge.Timer> gaugeTimer = new ThreadLocal<>();
    private static final Gauge UTX_SQL_TIME_SECONDS_TOTAL = buildGaugeForSql("utx_sql_time_seconds_total", "Total seconds spent executing sql.");
    private static final Counter UTX_SQL_TOTAL = buildCounterForSql("utx_sql_total", "SQL total number.");

    private static boolean isEnableMonitorServer = false;// if the property 'utx.prometheus.metrics.port' has a valid value, then it is true. true: enable monitor, false: disable monitor

    private static Counter buildCounter(String name, String help) {
//        return Counter.build(name, help).labelNames(labelNames).register();// got an error in using the labelNames variable case.
        return Counter.build(name, help).labelNames("business", "category").register();
    }

    private static Gauge buildGauge(String name, String help) {
        return Gauge.build(name, help).labelNames("business", "category").register();
    }

    private static Gauge buildGaugeForSql(String name, String help) {
        return Gauge.build(name, help).labelNames("bizsql", "business", "category").register();
    }

    private static Counter buildCounterForSql(String name, String help) {
        return Counter.build(name, help).labelNames("bizsql", "business", "category").register();
    }

    public UtxMetrics(String promMetricsPort) {
        try {
            DefaultExports.initialize();
            // Default port logic: the default port 8099 has been configured in application.yaml, thus if it's not 8099, then indicate that someone edited it automatically.
            if (promMetricsPort != null && promMetricsPort.length() > 0) {
                int metricsPort = Integer.parseInt(promMetricsPort);
                if (metricsPort > 0) {
                    // Initialize Prometheus's Metrics Server.
                    new HTTPServer(metricsPort);// To establish the metrics server immediately without checking the port status.
                    isEnableMonitorServer = true;
                }
            }
            this.register();
        } catch (IOException e) {
            LOG.error("Initialize utx metrics server exception, please check the port " + promMetricsPort + ". " + e);
        }

    }

    // Refer to the website "https://github.com/VitaNuovaR/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
    @Override
    public List<MetricFamilySamples> collect() {
        System.out.println("Fetch UTX Metrics -  " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()));
        List<MetricFamilySamples> metricList = new ArrayList<>();

        // expose the 'utx_transaction_total' metric.
//        metricList.add(new GaugeMetricFamily("utx_transaction_total", "Total number of transactions", eventRepository.totalTransaction()));
        return metricList;
    }

    public void countTxNumber(TxEvent event, boolean isTimeout, boolean isRetried) {
        if (!isEnableMonitor(event)) return;
        try {
            Set<String> eventTypesOfCurrentTx = globalTxIdAndTypes.get(event.globalTxId());
            if (eventTypesOfCurrentTx == null) {
                eventTypesOfCurrentTx = new HashSet<>();
            }
            String type = event.type(), serviceName = event.serviceName(), category = event.category();
            if (!eventTypesOfCurrentTx.contains(type)) {
                eventTypesOfCurrentTx.add(type);
                globalTxIdAndTypes.put(event.globalTxId(), eventTypesOfCurrentTx);

                if (SagaStartedEvent.name().equals(type)) {
                    UTX_TRANSACTION_TOTAL.labels(serviceName, category).inc();// ps: it would not appear in the metrics page if didn't set the labels' values.
                } else if (SagaEndedEvent.name().equals(type)) {
                    UTX_TRANSACTION_SUCCESSFUL_TOTAL.labels(serviceName, category).inc();
                    globalTxIdAndTypes.remove(event.globalTxId());
                    return;
                } else if (TxAbortedEvent.name().equals(type)) {
                    UTX_TRANSACTION_FAILED_TOTAL.labels(serviceName, category).inc();
                } else if (TxCompensatedEvent.name().equals(type)) {
                    UTX_TRANSACTION_ROLLBACKED_TOTAL.labels(serviceName, category).inc();
                } else if (AdditionalEventType.SagaPausedEvent.name().equals(type)) {
                    UTX_TRANSACTION_PAUSED_TOTAL.labels(serviceName, category).inc();
                    return;
                } else if (AdditionalEventType.SagaContinuedEvent.name().equals(type)) {
                    UTX_TRANSACTION_CONTINUED_TOTAL.labels(serviceName, category).inc();
                    return;
                } else if (AdditionalEventType.SagaAutoContinuedEvent.name().equals(type)) {
                    UTX_TRANSACTION_AUTOCONTINUED_TOTAL.labels(serviceName, category).inc();
                    return;
                }
            }

            // handle retried transaction
            if (isTimeout && !eventTypesOfCurrentTx.contains("TxTimeoutEvent")) {
                eventTypesOfCurrentTx.add("TxTimeoutEvent");
                globalTxIdAndTypes.put(event.globalTxId(), eventTypesOfCurrentTx);
                UTX_TRANSACTION_TIMEOUT_TOTAL.labels(serviceName, category).inc();
            }

            // handle retried transaction. ps: do not support retries in timeout case.
            if (isRetried && !eventTypesOfCurrentTx.contains("TxRetriedEvent")) {
                eventTypesOfCurrentTx.add("TxRetriedEvent");
                globalTxIdAndTypes.put(event.globalTxId(), eventTypesOfCurrentTx);
                UTX_TRANSACTION_RETRIED_TOTAL.labels(serviceName, category).inc();
            }
        } catch (Exception e) {
            LOG.error("Count utx transaction number exception: " + e);
        }
    }

    public void countChildTxNumber(TxEvent event) {
        if (!isEnableMonitor(event)) return;
        if (TxStartedEvent.name().equals(event.type()) && !localTxIdSet.contains(event.localTxId())) {
            UTX_TRANSACTION_CHILD_TOTAL.labels(event.serviceName(), event.category()).inc();
            localTxIdSet.add(event.localTxId());
        } else if (TxEndedEvent.name().equals(event.type())) {
            localTxIdSet.remove(event.localTxId());
        }
    }

    public void startMarkTxDuration(TxEvent event) {
        if (!isEnableMonitor(event)) return;
        // Start a timer to track a duration, for the gauge with no labels. So, we must set the value of the labelNames property.
        if (SagaStartedEvent.name().equals(event.type())) {
            txIdAndGaugeTimer.put(event.globalTxId(), UTX_TRANSACTION_TIME_SECONDS_TOTAL.labels(event.serviceName(), event.category()).startTimer());
        } else if (TxStartedEvent.name().equals(event.type())) {
            txIdAndGaugeTimer.put(event.localTxId(), UTX_TRANSACTION_CHILD_TIME_SECONDS_TOTAL.labels(event.serviceName(), event.category()).startTimer());
        }
    }

    public void endMarkTxDuration(TxEvent event) {
        if (!isEnableMonitor(event)) return;
        String globalOrLocalTxId = "";
        if (SagaEndedEvent.name().equals(event.type())) {
            globalOrLocalTxId = event.globalTxId();
        } else if (TxEndedEvent.name().equals(event.type())) {
            globalOrLocalTxId = event.localTxId();
        }
        Gauge.Timer gaugeTimer = txIdAndGaugeTimer.get(globalOrLocalTxId);
        if (gaugeTimer != null) {
            gaugeTimer.setDuration();
            txIdAndGaugeTimer.remove(globalOrLocalTxId);
        }
    }

    public String startMarkSQLDurationAndCount(String sql, boolean isJpaStandard, Object[] args) {
        if (!isEnableMonitorServer) return "";

        String globalTxId = null;
        TxEvent event = null;
        for (Object arg : args) {
            event = CurrentThreadContext.get(arg + "");
            if (event != null) {
                break;
            }
        }
        if (event == null && isJpaStandard && args.length > 0) {
            if (args[0] instanceof TxEvent) {
                event = (TxEvent) args[0];
            } else if (args[0] instanceof TxTimeout) {// TODO serviceName, category
            } else if (args[0] instanceof Command) {// TODO
            }
        }

        String serviceName = "", category = "";
        if (event != null) {// If event is null, then current statistic will be classified as default group.
            serviceName = event.serviceName();
            category = event.category();
            globalTxId = event.globalTxId();
        }
        gaugeTimer.set(UTX_SQL_TIME_SECONDS_TOTAL.labels(false + "", serviceName, category).startTimer());
        // 成功情况2条，需要回滚前查provide1，记录p1待补偿命令，下p1补偿，更新待补偿命令为done，记录p1对应的SagaEndedEvent，共7条。
        UTX_SQL_TOTAL.labels(false + "", serviceName, category).inc();

        return globalTxId;
    }

    public void endMarkSQLDuration(String globalTxId) {
        if (!isEnableMonitorServer) return;
        Gauge.Timer timer = gaugeTimer.get();
        if (timer != null) {
            timer.setDuration();
        }
        gaugeTimer.remove();
        CurrentThreadContext.clearCache(globalTxId);
    }

    private boolean isEnableMonitor(TxEvent event) {
        if (!isEnableMonitorServer) return false;
        return dbDegradationConfigService.isEnabledTx(event.instanceId(), ConfigCenterType.TxMonitor);
    }

}
