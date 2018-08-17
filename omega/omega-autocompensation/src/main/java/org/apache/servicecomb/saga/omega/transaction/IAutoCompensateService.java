package org.apache.servicecomb.saga.omega.transaction;

import java.util.LinkedList;

/**
 * Business interface for auto-compensation.
 * 
 * @author Gannalyo
 * @since 2018-07-30
 */
public interface IAutoCompensateService {
	
	/**
	 * To construct and save auto-compensation info by executed SQL list at the end of one sub-transaction.
	 * 
	 * @param globalTxId Global Transaction Identify
	 * @param localTxId Sub-transaction Identify
	 * @param executeSqlList list of executed SQL
	 * @param originalInfoList list of original info
	 * @param server save ip:port
	 * @return result
	 * @author Gannalyo
	 * @since 2018-07-30
	 */
	public boolean saveAutoCompensableInfo(String globalTxId, String localTxId, LinkedList<String> executeSqlList, LinkedList<String> originalInfoList, String server);
	
	/**
	 * To execute auto-compensation SQL in case of system exception. 
	 * 
	 * @param globalTxId Global Transaction Identify
	 * @param localTxId Sub-transaction Identify
	 * @return result
	 * @author Gannalyo
	 * @since 2018-07-30
	 */
	public boolean executeAutoCompensateByLocalTxId(String globalTxId, String localTxId);
}
