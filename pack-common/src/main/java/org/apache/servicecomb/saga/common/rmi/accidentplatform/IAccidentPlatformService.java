package org.apache.servicecomb.saga.common.rmi.accidentplatform;

/**
 * Accident Platform Interface.
 * 
 * @author Gannalyo
 * @since 2018-08-27
 */
public interface IAccidentPlatformService {
	boolean reportMsgToAccidentPlatform(String jsonParams);
}
