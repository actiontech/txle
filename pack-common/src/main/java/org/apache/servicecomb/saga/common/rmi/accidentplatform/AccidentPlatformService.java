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
    public boolean reportMsgToAccidentPlatform(AccidentType type, String globalTxId, String localTxId) {
        LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Message [type=[{}], globalTxId=[{}], localTxId=[{}]] will send to Accident Platform [" + this.accidentPlatformAddress + "].", type.name(), globalTxId, localTxId);
        return false;
    }

}