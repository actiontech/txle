package org.apache.servicecomb.saga.omega.rmi.accidentplatform;

import org.apache.servicecomb.saga.omega.context.UtxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
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