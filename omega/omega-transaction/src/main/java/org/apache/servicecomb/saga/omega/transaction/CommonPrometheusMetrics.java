package org.apache.servicecomb.saga.omega.transaction;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.monitor.CompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author Gannalyo
 * @date 2019/3/8
 */
public class CommonPrometheusMetrics extends Collector {
    private static final Logger LOG = LoggerFactory.getLogger(CompensableSqlMetrics.class);

    // TODO The value of 'Counter' will become zero after restarting current application.
    // mark duration
    // To store 'globalTxId' for aborted events, it is the aim to avoid counting repeat event number. Do not need to pay more attention on restart, cluster and concurrence.
    protected static final Gauge UTX_SQL_TIME_SECONDS_TOTAL = buildGauge("utx_sql_time_seconds_total", "Total seconds spent executing sql.");
    protected static final ThreadLocal<Gauge.Timer> gaugeTimer = new ThreadLocal<>();

    protected static final Counter UTX_SQL_TOTAL = buildCounter("utx_sql_total", "SQL total number.");
    private static boolean httpServer = false;
    protected static volatile boolean isMonitorSql = true;

    public CommonPrometheusMetrics(String promMetricsPort) {
        try {
            if (httpServer) return;
            // Default port logic: the default port 8098 if it's null. If not null, use the config value.
            int metricsPort = Integer.parseInt(promMetricsPort);
            if (metricsPort > 0) {
                // Initialize Prometheus's Metrics Server.
                new HTTPServer(metricsPort);// To establish the metrics server immediately without checking the port status.
                httpServer = true;
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

    public void startMarkSQLDurationAndCount(String sql, boolean isBizSql) {
        OmegaContextServiceConfig context = CurrentThreadOmegaContext.getContextFromCurThread();
        String serviceName = "", category = "";
        if (isBizSql && context != null) {
            serviceName = handleStringNullValue(context.serviceName());
            category = handleStringNullValue(context.category());
        }

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

    public void endMarkSQLDuration() {
        Gauge.Timer timer = gaugeTimer.get();
        if (timer != null) {
            timer.setDuration();
        }
        gaugeTimer.remove();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return null;
    }

    public static void setIsMonitorSql(boolean isEnabled) {
        isMonitorSql = isEnabled;
    }
}
