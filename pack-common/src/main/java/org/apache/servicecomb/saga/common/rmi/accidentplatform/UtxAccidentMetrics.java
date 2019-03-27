package org.apache.servicecomb.saga.common.rmi.accidentplatform;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
 * @author Gannalyo
 * @date 20190326
 */
public class UtxAccidentMetrics extends Collector {

    // TODO The value of 'Counter' will become zero after restarting current application.
    // ps: Support cluster mode, in the cluster cases, to distinguish every instance by labelNames. Please view the prometheus.yml
    // Such as: utx_transaction_total{instance=~"utx8099",job=~"utx"} or utx_transaction_total{instance=~"utx8099",job=~"utx"}, summary is: sum(utx_transaction_total{job=~"utx"})
    private static final Gauge UTX_REPORT_ACCIDENT_SUCCESSFUL_TOTAL = buildGauge("utx_report_accident_successful_total", "Successful total number for reporting accident.");
    private static final Gauge UTX_REPORT_ACCIDENT_FAILED_TOTAL = buildGauge("utx_report_accident_failed_total", "Failed total number for reporting accident.");

    private static Gauge buildGauge(String name, String help) {
        return Gauge.build(name, help).register();
    }

    // Refer to the website "https://github.com/VitaNuovaR/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
    @Override
    public List<MetricFamilySamples> collect() {
        System.out.println("Fetch UTX Metrics -  " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()));
        List<MetricFamilySamples> metricList = new ArrayList<>();
        return metricList;
    }

    public static void countSuccessfulNumber() {
        UTX_REPORT_ACCIDENT_SUCCESSFUL_TOTAL.inc();
    }

    public static synchronized void countFailedNumber() {
        UTX_REPORT_ACCIDENT_FAILED_TOTAL.inc();
    }

}
