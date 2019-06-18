package org.apache.servicecomb.saga.alpha.core.accidenthandling;

/**
 * An enum for the status of accident handling.
 * 发送中、发送成功、发送失败、处理中、(处理)成功、(处理)失败
 *
 * @author Gannalyo
 * @date 2019/06/14
 */
public enum AccidentHandleStatus {
    SENDING,
    SEND_OK,
    SEND_FAIL,
    //    PROCESSING,
    SUCCESSFUL,
    FAILED;

    public int toInteger() {
        switch (this) {
            case SENDING:
                return 0;
            case SEND_OK:
                return 1;
            case SEND_FAIL:
                return 2;
            case SUCCESSFUL:
                return 3;
            case FAILED:
                return 4;
            default:
                return 0;
        }
    }

    public String toDescription() {
        switch (this) {
            case SENDING:
                return "发送中";
            case SEND_OK:
                return "发送成功";
            case SEND_FAIL:
                return "发送失败";
            case SUCCESSFUL:
                return "成功";
            case FAILED:
                return "失败";
            default:
                return "发送中";
        }
    }

    public AccidentHandleStatus convertStatusFromValue(int status) {
        switch (status) {
            case 0:
                return SENDING;
            case 1:
                return SEND_OK;
            case 2:
                return SEND_FAIL;
            case 3:
                return SUCCESSFUL;
            case 4:
                return FAILED;
            default:
                return SENDING;
        }
    }
}
