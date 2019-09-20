/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.datatransfer;

import org.apache.servicecomb.saga.alpha.core.StartingTask;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenter;
import org.apache.servicecomb.saga.alpha.core.configcenter.ConfigCenterStatus;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.datatransfer.IDataTransferService;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * This tool class just likes a simple ETL. For transferring normal data to some history tables according to some rule.
 *
 * @author Gannalyo
 * @since 2019/7/23
 */
public class DataTransferService implements IDataTransferService {
    private static final Logger LOG = LoggerFactory.getLogger(DataTransferService.class);

    private DataTransferRepository dataTransferRepository;
    private TxEventRepository txEventRepository;

    @Autowired
    private IConfigCenterService configCenterService;

    @Autowired
    private StartingTask startingTask;

    public DataTransferService(DataTransferRepository dataTransferRepository, TxEventRepository txEventRepository) {
        this.dataTransferRepository = dataTransferRepository;
        this.txEventRepository = txEventRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledTask() {
        // To transfer data on master node only.
        if (startingTask.isMaster()) {
            LOG.info("Triggered data transfer task on current master node.");
            dataTransfer("TxEvent");
        } else {
            LOG.info("Could not trigger data transfer task, because current node had been not master yet.");
        }
    }

    @Override
    public void dataTransfer(String srcTable) {
        /**
         * logic for data transfer
         * 0.support to configure the transfer rule, the value includes Day, Month, Season and Year, default Month.
         * 1.find the minimum date.
         * 2.read data refers SQL 【SELECT T.surrogateId FROM TxEvent T WHERE T.creationTime BETWEEN ?1 AND ?2 AND EXISTS (SELECT 1 FROM TxEvent T1
         *      WHERE T1.type = 'SagaEndedEvent' AND FUNCTION('TO_DAYS', CURRENT_TIMESTAMP) - FUNCTION('TO_DAYS', T1.creationTime) > 10 AND T.globalTxId = T1.globalTxId)】.
         * 3.create corresponding table if it is not exists.
         * 4.copy data
         * 5.delete data
         */
        int historyTableInternalRule = 1;
        List<ConfigCenter> dataTransferFrequencyList = configCenterService.selectConfigCenterByType(null, null, ConfigCenterStatus.Normal.toInteger(), ConfigCenterType.HistoryTableIntervalRule.toInteger());
        if (dataTransferFrequencyList != null && !dataTransferFrequencyList.isEmpty()) {
            String value = dataTransferFrequencyList.get(0).getValue();
            if (value != null && value.trim().length() > 0) {
                historyTableInternalRule = Integer.parseInt(value.trim());
            }
        }

        switch (historyTableInternalRule) {
            case 0:
                LOG.info("Transferring data with the rule '0'.");
                transferDataByDayMonthYear(srcTable, "yyyyMMdd");
                return;
            case 2:
                LOG.info("Transferring data with the rule '2'.");
                transferDataBySeason(srcTable);
                return;
            case 3:
                LOG.info("Transferring data with the rule '3'.");
                transferDataByDayMonthYear(srcTable, "yyyy");
                return;
            default:
                LOG.info("Transferring data with the rule '1'.");
                transferDataByDayMonthYear(srcTable, "yyyyMM");
                return;
        }
    }

    private void transferDataByDayMonthYear(String srcTable, String datePattern) {
        Date minDate = txEventRepository.selectMinDateInTxEvent();
        if (minDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
            int minYMD = Integer.parseInt(sdf.format(minDate));
            int curYMD = Integer.parseInt(sdf.format(new Date()));
            LOG.info("Transferring data, min date [{}], current date [{}].", minYMD, curYMD);
            for (int i = minYMD; i <= curYMD; ) {
                try {
                    Date startTime = sdf.parse(i + "");
                    Date endTime = sdf.parse((i + 1) + "");

                    moveDataToHistory(srcTable, i + "", startTime, endTime);

                    i = increaseDate(datePattern, i);
                } catch (Exception e) {
                    LOG.info("Encountered an error in case of transferring data, date [{}].", i, e);
                }
            }
        }
    }

    private int increaseDate(String datePattern, int date) {
        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdfYMD = new SimpleDateFormat(datePattern);
            calendar.setTime(sdfYMD.parse(date + ""));
            calendar.add("yyyy".equals(datePattern) ? Calendar.YEAR : "yyyyMM".equals(datePattern) ? Calendar.MONTH : Calendar.DAY_OF_YEAR, 1);
            return Integer.parseInt(sdfYMD.format(calendar.getTime()));
        } catch (Exception e) {
            LOG.info("Failed to increase the date, datePattern [{}], date [{}].", datePattern, date, e);
        }
        return date;
    }

    // Season：1-3、4-6、7-9、10-12
    private void transferDataBySeason(String srcTable) {
        Date minDate = txEventRepository.selectMinDateInTxEvent();
        if (minDate != null) {
            // format: yyyyMM
            int minSeason = computeSeason(minDate);
            int curSeason = computeSeason(new Date());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
            for (int i = minSeason; i <= curSeason; i = String.valueOf(i).endsWith("4") ? i + 100 - 3 : i + 1) {
                try {
                    String year = String.valueOf(i).substring(0, 4);
                    int season = Integer.parseInt(String.valueOf(i).substring(5));
                    Date startTime = sdf.parse(year + "0" + (3 * (season - 1) + 1));
                    Date endTime = sdf.parse(year + "0" + (3 * (season - 1) + 4));

                    // Selecting And Moving Season Data.
                    moveDataToHistory(srcTable, i + "season", startTime, endTime);
                } catch (Exception e) {
                    LOG.info("Encountered an error in case of transferring data, date [{}].", i, e);
                }
            }
        }
    }

    private String convertMonthToSeason(int month, int year) {
        switch (month) {
            case 1:
                return year + "01";
            case 2:
                return year + "01";
            case 3:
                return year + "01";
            case 4:
                return year + "02";
            case 5:
                return year + "02";
            case 6:
                return year + "02";
            case 7:
                return year + "03";
            case 8:
                return year + "03";
            case 9:
                return year + "03";
            default:
                return year + "04";
        }
    }

    private int computeSeason(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        int month = Integer.parseInt(sdf.format(date).substring(4));
        int year = Integer.parseInt(sdf.format(date).substring(0, 4));
        return Integer.parseInt(convertMonthToSeason(month, year));
    }

    private void createHistoryTable(String srcTable, String suffix) {
        dataTransferRepository.executeUpdate("CREATE TABLE IF NOT EXISTS " + srcTable + "_" + suffix + " LIKE " + srcTable);
    }

    private void moveDataToHistory(String srcTable, String suffix, Date startTime, Date endTime) {
        int pageIndex = 0, pageSize = 10000;
        while (true) {
            // read 1000 rows data every time to avoid that too much data eat up memory.
            // The data of following variable will be removed from database. So, the variable 'pageIndex' will be always 0, has no need increase.
            List<Long> eventIdList = txEventRepository.selectEndedEventIdsWithinSomePeriod(pageIndex, pageSize, startTime, endTime);
            LOG.info("Transferring data, get data from method 'selectEndedEventIdsWithinSomePeriod', data size [{}].", eventIdList == null ? 0 : eventIdList.size());
            if (eventIdList == null || eventIdList.isEmpty()) {
                break;
            }

            // Creating Season Table.
            createHistoryTable(srcTable, suffix);
            LOG.info("Transferring data, create successfully.");

            moveDataToHistory(srcTable, suffix, eventIdList);
            LOG.info("Transferring data, move successfully.");

            int eventSize = eventIdList.size();
            eventIdList.clear();
            eventIdList = null;
            if (eventSize < pageSize) {
                break;
            }
        }
    }

    private void moveDataToHistory(String srcTable, String suffix, List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }

        int maxPlaceholders = 1000;
        StringBuilder insertSql = new StringBuilder("INSERT IGNORE INTO " + srcTable + "_" + suffix + " SELECT * FROM " + srcTable + " T WHERE T.surrogateId IN (");
        StringBuilder deleteSql = new StringBuilder("DELETE FROM " + srcTable + " WHERE surrogateId IN (");
        if (idList.size() < maxPlaceholders) {
            for (int i = 0; i < idList.size(); i++) {
                if (i == 0) {
                    insertSql.append("?");
                    deleteSql.append("?");
                } else {
                    insertSql.append(",?");
                    deleteSql.append(",?");
                }
            }
            insertSql.append(")");
            deleteSql.append(")");

            dataTransferRepository.executeUpdate(insertSql.toString(), idList.toArray());
            dataTransferRepository.executeUpdate(deleteSql.toString(), idList.toArray());
        } else {
            StringBuilder placeholders = new StringBuilder(maxPlaceholders);
            List<Long> idSetWithin1000 = new LinkedList<>();
            for (int i = 1; i < idList.size() + 1; i++) {
                idSetWithin1000.add(idList.get(i - 1));
                placeholders.append("?,");

                if (i % maxPlaceholders == 0 || i == idList.size()) {
                    placeholders = new StringBuilder(placeholders.substring(0, placeholders.length() - 1) + ")");
                    dataTransferRepository.executeUpdate(insertSql.toString() + placeholders, idSetWithin1000.toArray());
                    dataTransferRepository.executeUpdate(deleteSql.toString() + placeholders, idSetWithin1000.toArray());
                    idSetWithin1000.clear();
                    placeholders = new StringBuilder(maxPlaceholders);
                }
            }
            placeholders = null;
            idSetWithin1000 = null;
        }
    }

}
