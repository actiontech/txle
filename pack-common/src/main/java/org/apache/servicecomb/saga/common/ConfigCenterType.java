/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.common;

/**
 * if values are less than 50, then configs for server, otherwise configs for client.
 */
public enum ConfigCenterType {
    // All of values except fault-tolerant are enabled or disabled in database.
    GlobalTx,
    Compensation,
    AutoCompensation,
    BizInfoToKafka,
    TxMonitor,
    Alert,
    Schedule,
    GlobalTxFaultTolerant,
    CompensationFaultTolerant,
    AutoCompensationFaultTolerant,
    PauseGlobalTx,
    TerminateGlobalTx,
    HistoryTableIntervalRule,
    AccidentReport,
    SqlMonitor,
    ClientCompensate;

    public int toInteger() {
        switch (this) {
            case GlobalTx:
                return 1;
            case Compensation:
                return 2;
            case AutoCompensation:
                return 3;
            case BizInfoToKafka:
                return 4;
            case TxMonitor:
                return 5;
            case Alert:
                return 6;
            case Schedule:
                return 7;
            case GlobalTxFaultTolerant:
                return 8;
            case CompensationFaultTolerant:
                return 9;
            case AutoCompensationFaultTolerant:
                return 10;
            case PauseGlobalTx:
                return 11;
            case HistoryTableIntervalRule:
                return 12;
            case TerminateGlobalTx:
                return 13;
            case AccidentReport:
                return 50;
            case SqlMonitor:
                return 51;
            case ClientCompensate:
                return 52;
            default:
                return 1;
        }
    }

    public String toDescription() {
        switch (this) {
            case GlobalTx:
                return "全局事务";
            case Compensation:
                return "手动补偿";
            case AutoCompensation:
                return "自动补偿";
            case BizInfoToKafka:
                return "业务信息上报";
            case TxMonitor:
                return "事务监控";
            case Alert:
                return "告警";
            case Schedule:
                return "定时任务";
            case GlobalTxFaultTolerant:
                return "全局事务容错";
            case CompensationFaultTolerant:
                return "手动补偿容错";
            case AutoCompensationFaultTolerant:
                return "自动补偿容错";
            case PauseGlobalTx:
                return "暂停全局事务";
            case HistoryTableIntervalRule:
                return "历史表间隔规则";
            case TerminateGlobalTx:
                return "终止全局事务";
            case AccidentReport:
                return "差错上报";
            case SqlMonitor:
                return "SQL监控";
            case ClientCompensate:
                return "客户端补偿";
            default:
                return "全局事务";
        }
    }

    public static ConfigCenterType convertTypeFromValue(int type) {
        switch (type) {
            case 1:
                return GlobalTx;
            case 2:
                return Compensation;
            case 3:
                return AutoCompensation;
            case 4:
                return BizInfoToKafka;
            case 5:
                return TxMonitor;
            case 6:
                return Alert;
            case 7:
                return Schedule;
            case 8:
                return GlobalTxFaultTolerant;
            case 9:
                return CompensationFaultTolerant;
            case 10:
                return AutoCompensationFaultTolerant;
            case 11:
                return PauseGlobalTx;
            case 12:
                return HistoryTableIntervalRule;
            case 13:
                return TerminateGlobalTx;
            case 50:
                return AccidentReport;
            case 51:
                return SqlMonitor;
            case 52:
                return ClientCompensate;
            default:
                throw new RuntimeException("No type value for " + type + ".");
        }
    }

    public boolean defaultValue() {
        switch (this) {
            case GlobalTx:
                return true;
            case Compensation:
                return true;
            case AutoCompensation:
                return true;
            case BizInfoToKafka:
                return false;
            case TxMonitor:
                return true;
            case Alert:
                return false;
            case Schedule:
                return true;
            case GlobalTxFaultTolerant:
                return false;
            case CompensationFaultTolerant:
                return false;
            case AutoCompensationFaultTolerant:
                return false;
            case PauseGlobalTx:
                return false;
            case AccidentReport:
                return true;
            case TerminateGlobalTx:
                return false;
            case SqlMonitor:
                return true;
            case ClientCompensate:
                return false;
            default:
                return true;
        }
    }

    public int defaultIntValue() {
        switch (this) {
            // 历史表间隔规则。值：0-日，1-月，2-季，3-年。注：不转储10天内的数据。
            case HistoryTableIntervalRule:
                return 1;
            default:
                return -1;
        }
    }
}
