package org.apache.servicecomb.saga.common.rmi.accidentplatform;

import org.apache.servicecomb.saga.common.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccidentPlatformService implements IAccidentPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(AccidentPlatformService.class);
    private String accidentPlatformAddress;

    public AccidentPlatformService(String accidentPlatformAddress) {
        this.accidentPlatformAddress = accidentPlatformAddress;
    }

    @Override
    public boolean reportMsgToAccidentPlatform(String msg) {
        LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Message [" + msg + "] will send to Accident Platform [" + this.accidentPlatformAddress + "].");
        return false;
    }

}