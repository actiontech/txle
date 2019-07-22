package org.apache.servicecomb.saga.alpha.core.accidenthandling;

/**
 * An enum for the status of accident handling.
 * 发送中、发送成功、发送失败、处理成功、处理失败
 *
 * @author Gannalyo
 * @date 2019/06/14
 */
public enum AccidentHandleStatus {
    SENDING,
    SEND_OK,
    SEND_FAIL,
    //    PROCESSING,
    HANDLE_OK,
    HANDLE_FAIL;

    public int toInteger() {
        switch (this) {
            case SENDING:
                return 0;
            case SEND_OK:
                return 1;
            case SEND_FAIL:
                return 2;
            case HANDLE_OK:
                return 3;
            case HANDLE_FAIL:
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
            case HANDLE_OK:
                return "处理成功";
            case HANDLE_FAIL:
                return "处理失败";
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
                return HANDLE_OK;
            case 4:
                return HANDLE_FAIL;
            default:
                return SENDING;
        }
    }
}
