package com.p6spy.engine.monitor;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

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
    }

    private static String handleStringNullValue(String param) {
        return param;
    }

    public static void endMarkSQLDuration() {
    }

}
