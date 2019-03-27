package org.apache.servicecomb.saga.common.rmi.accidentplatform;

/**
 * An enum for the accident type.
 *
 * @author Gannalyo
 * @date 2018/12/13
 */
public enum AccidentType {
    ROLLBACK_ERROR,
    SEND_MESSAGE_ERROR;

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

}
