package org.apache.servicecomb.saga.omega.rmi.accidentplatform;

/**
 * Accident Platform Interface.
 * 
 * @author Gannalyo
 * @since 2018-08-27
 */
public interface IAccidentPlatformService {
	// Information to accident platform. 
	public boolean reportMsgToAccidentPlatform(String msg);
}
