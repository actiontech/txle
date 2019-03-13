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
    AccidentReport,
    SqlMonitor;

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
            case AccidentReport:
                return 50;
            case SqlMonitor:
                return 51;
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
            case AccidentReport:
                return "差错上报";
            case SqlMonitor:
                return "SQL监控";
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
            case 50:
                return AccidentReport;
            case 51:
                return SqlMonitor;
            default:
                throw new RuntimeException("No type value for " + type + ".");
        }
    }
}
