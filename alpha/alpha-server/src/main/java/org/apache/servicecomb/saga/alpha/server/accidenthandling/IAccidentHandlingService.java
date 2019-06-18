package org.apache.servicecomb.saga.alpha.server.accidenthandling;

/**
 * Accident Platform Interface.
 * 
 * @author Gannalyo
 * @since 2018-08-27
 */
public interface IAccidentHandlingService {
	boolean reportMsgToAccidentPlatform(String jsonParams);
}
