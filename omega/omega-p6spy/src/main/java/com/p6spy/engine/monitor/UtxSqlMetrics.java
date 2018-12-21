package com.p6spy.engine.monitor;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
 *
 * @author Gannalyo
 * @date 20181024
 */
public class UtxSqlMetrics extends Collector {
    private static final Logger LOG = LoggerFactory.getLogger(UtxSqlMetrics.class);

    // TODO The value of 'Counter' will become zero after restarting current application.
    // mark duration
    // To store 'globalTxId' for aborted events, it is the aim to avoid counting repeat event number. Do not need to pay more attention on restart, cluster and concurrence.
    private static final Gauge UTX_SQL_TIME_SECONDS_TOTAL = buildGauge("utx_sql_time_seconds_total", "Total seconds spent executing sql.");
    private static final ThreadLocal<Gauge.Timer> gaugeTimer = new ThreadLocal<>();

    private static final Counter UTX_SQL_TOTAL = buildCounter("utx_sql_total", "SQL total number.");

    public UtxSqlMetrics(String promMetricsPort) {
        try {
            // Default port logic: the default port 8098 if it's null. If not null, use the config value.
            int metricsPort = Integer.parseInt(promMetricsPort);
            if (metricsPort > 0) {
                // Initialize Prometheus's Metrics Server.
                new HTTPServer(metricsPort);// To establish the metrics server immediately without checking the port status.
            }
        } catch (IOException e) {
            LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Initialize utx sql metrics server exception, please check the port " + promMetricsPort + ". " + e);
        }
    }

    private static Gauge buildGauge(String name, String help) {
        return Gauge.build(name, help).labelNames("bizsql", "business", "category").register();
    }

    private static Counter buildCounter(String name, String help) {
        return Counter.build(name, help).labelNames("bizsql", "business", "category").register();
    }

    // Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
    @Override
    public List<MetricFamilySamples> collect() {
        System.out.println("Fetch UTX SQL Metrics -  " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()));
        List<MetricFamilySamples> metricList = new ArrayList<>();
        return metricList;
    }

    public static void startMarkSQLDurationAndCount(String sql, boolean isBizSql) {
        OmegaContextServiceConfig context = CurrentThreadOmegaContext.getContextFromCurThread();
        String serviceName = "", category = "";
        if (isBizSql && context != null) {
            serviceName = handleStringNullValue(context.serviceName());
            category = handleStringNullValue(context.category());
        }

        System.out.println(Thread.currentThread().getId());
        // TODO If this method was invoked for many times in the same thread, then the later value will cover the early value.
        gaugeTimer.set(UTX_SQL_TIME_SECONDS_TOTAL.labels(isBizSql + "", serviceName, category).startTimer());
        UTX_SQL_TOTAL.labels(isBizSql + "", serviceName, category).inc();
    }

    private static String handleStringNullValue(String param) {
        if (param == null || "null".equalsIgnoreCase(param)) {
            param = "";
        }
        return param;
    }

    public static void endMarkSQLDuration() {
        Gauge.Timer timer = gaugeTimer.get();
        if (timer != null) {
            timer.setDuration();
        }
        gaugeTimer.remove();
    }

}
