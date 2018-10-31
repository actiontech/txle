package org.apache.servicecomb.saga.alpha.server.restapi;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
 * @author Gannalyo
 * @date 20181024
 */
@RestController
public class UtxMetrics extends Collector {

    @Autowired
    private TxEventRepository eventRepository;

    public UtxMetrics(TxEventRepository eventRepository) {
        if (this.eventRepository == null) {
            this.eventRepository = eventRepository;
        }

        try {
            DefaultExports.initialize();
            new HTTPServer(8099);// TODO 端口配置？？？
            this.register();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
    @Override
    public List<MetricFamilySamples> collect() {
        System.out.println("Fetch UTX Metrics -  " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()));
        List<MetricFamilySamples> metricList = new ArrayList<>();

        // expose the 'utx_transaction_total' metric.
        metricList.add(new GaugeMetricFamily("utx_transaction_total", "The total number of transactions", eventRepository.totalTransaction()));
        metricList.add(new GaugeMetricFamily("utx_transaction_failed_total", "The total number of failed transactions", eventRepository.totalFailedTransaction()));
        metricList.add(new GaugeMetricFamily("utx_transaction_rollbacked_total", "The total number of rollbacked transactions", eventRepository.totalRollbackedTransaction()));
        metricList.add(new GaugeMetricFamily("utx_transaction_retried_total", "The total number of retried transactions", eventRepository.totalRetriedTransaction()));
        metricList.add(new GaugeMetricFamily("utx_transaction_timeout_total", "The total number of timeout transactions", eventRepository.totalTimeoutTransaction()));

        return metricList;
    }

}
