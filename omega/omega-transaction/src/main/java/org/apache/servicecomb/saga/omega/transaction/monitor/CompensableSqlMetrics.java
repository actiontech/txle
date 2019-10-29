/*
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.monitor;

import java.util.List;

/**
 * Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
 *
 * @author Gannalyo
 * @since 20181024
 */
public class CompensableSqlMetrics extends CommonPrometheusMetrics {

    public CompensableSqlMetrics(String promMetricsPort) {
        super(promMetricsPort);
    }

    // Refer to the website "https://github.com/VitaNuova/eclipselinkexporter/blob/master/src/main/java/prometheus/exporter/EclipseLinkStatisticsCollector.java".
    @Override
    public List<MetricFamilySamples> collect() {
        return null;
    }

    public void startMarkSQLDurationAndCount(String sql, boolean isBizSql) {
        if (!isMonitorSql()) {
            return;
        }
        super.startMarkSQLDurationAndCount(sql, isBizSql);
    }

    public void endMarkSQLDuration() {
        super.endMarkSQLDuration();
    }

}
