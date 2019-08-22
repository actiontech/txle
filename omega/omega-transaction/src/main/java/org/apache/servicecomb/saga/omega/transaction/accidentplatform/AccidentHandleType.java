package org.apache.servicecomb.saga.omega.transaction.accidentplatform;

/**
 * An enum for the type of accident handling.
 *
 * @author Gannalyo
 * @since 2019/06/14
 */
public enum AccidentHandleType {
    ROLLBACK_ERROR,
    SEND_MESSAGE_ERROR;

    public int toInteger() {
        switch (this) {
            case ROLLBACK_ERROR:
                return 1;
            case SEND_MESSAGE_ERROR:
                return 2;
            default:
                return 1;
        }
    }

    public String toDescription() {
        switch (this) {
            case ROLLBACK_ERROR:
                return "回滚失败";
            case SEND_MESSAGE_ERROR:
                return "上报信息至Kafka失败";
            default:
                return "回滚失败";
        }
    }

    public AccidentHandleType convertTypeFromValue(int type) {
        switch (type) {
            case 1:
                return ROLLBACK_ERROR;
            case 2:
                return SEND_MESSAGE_ERROR;
            default:
                return ROLLBACK_ERROR;
        }
    }

    public static AccidentHandleType convertTypeFromDescription(String description) {
        if ("上报信息至Kafka失败".equals(description)) {
            return SEND_MESSAGE_ERROR;
        }
        return ROLLBACK_ERROR;
    }
}
