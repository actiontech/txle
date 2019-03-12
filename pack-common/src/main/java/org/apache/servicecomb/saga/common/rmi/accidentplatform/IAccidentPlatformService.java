package org.apache.servicecomb.saga.common.rmi.accidentplatform;

/**
 * Accident Platform Interface.
 * 
 * @author Gannalyo
 * @since 2018-08-27
 */
public interface IAccidentPlatformService {
	// Information to accident platform. TODO 架构是否正确，存放位置是否需要依据server和client进行调整？？？
	boolean reportMsgToAccidentPlatform(AccidentType type, String globalTxId, String localTxId);
}
