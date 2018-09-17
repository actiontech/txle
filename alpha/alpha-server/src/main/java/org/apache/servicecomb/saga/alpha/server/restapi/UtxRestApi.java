package org.apache.servicecomb.saga.alpha.server.restapi;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.servicecomb.saga.alpha.core.AdditionalEventType;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UtxRestApi {
	private static final Logger LOG = LoggerFactory.getLogger(UtxRestApi.class);

	@Autowired
	private TxEventRepository eventRepository;
	
	private TxConsistentService txConsistentService;

	@Autowired
	private HttpServletRequest request;
	
	public UtxRestApi(TxConsistentService txConsistentService) {
		this.txConsistentService = txConsistentService;
	}
	
	public List<Map<String, Object>> selectAllTransactions() {
		// TODO
		return null;
	}

	public List<Map<String, Object>> selectOvertimeTransactions() {
		// TODO
		return null;
	}

	public List<Map<String, Object>> selectRunningTransactions() {
		// TODO
		return null;
	}

	public List<Map<String, Object>> selectPausedTransactions() {
		// TODO
		return null;
	}

	public List<Map<String, Object>> selectRolledbackTransactions() {
		// TODO
		return null;
	}

	public List<Map<String, Object>> selectHistoricalTransactions() {
		// TODO
		return null;
	}

	/**
	 * To pause specified global transaction by identify.
	 * 
	 * @param globalTxId identify for global transaction
	 * @return
	 */
	@GetMapping("/pauseGlobalTransactionById/{globalTxId}")
	public String pauseGlobalTransactionById(@PathVariable long globalTxId) {
		return pauseGlobalTransactionById(globalTxId, 0);
	}

	/**
	 * To pause specified global transaction by identify.
	 * 
	 * @param globalTxId identify for global transaction
	 * @param pausePeriod The global transaction will continue to run in 'pausePeriod' seconds automatically. It will not continue to if the value of its pausePeriod is 0. The former is the paused must be true;
	 * @return
	 */
	@GetMapping("/pauseGlobalTransactionById/{globalTxId}/{pausePeriod}")
	public String pauseGlobalTransactionById(@PathVariable long globalTxId, @PathVariable int pausePeriod) {
		if (pausePeriod < 0) {
			pausePeriod = 0;
		}
		LOG.debug("Executing the 'pauseGlobalTransactionById' API - globalTxId [{}], pausePeriod [{}].", globalTxId, pausePeriod);
		return pauseOrContinueGlobalTransactionById(globalTxId, pausePeriod, AdditionalEventType.SagaPausedEvent, "pause");
	}

	@GetMapping("/continueGlobalTransactionById/{globalTxId}")
	public String continueGlobalTransactionById(@PathVariable long globalTxId) {
		LOG.debug("Executing the 'continueGlobalTransactionById' API - globalTxId [{}].", globalTxId);
		return pauseOrContinueGlobalTransactionById(globalTxId, 0, AdditionalEventType.SagaContinuedEvent, "continue");
	}
	
	private String pauseOrContinueGlobalTransactionById(long globalTxId, int pausePeriod, AdditionalEventType additionalEventType, String operation) {
		boolean pauseOperation = additionalEventType.equals(AdditionalEventType.SagaPausedEvent);
		TxEvent txEvent = eventRepository.findOne(globalTxId);
		if (txEvent == null) {
			return "Fail to " + operation + " - could not find transaction by globalTxId [" + globalTxId + "].";
		}
		
		// To verify if current transaction had been over.
		List<TxEvent> endedTransactions = eventRepository.findTransactions(txEvent.globalTxId(), EventType.SagaEndedEvent.name());
		if (endedTransactions != null && !endedTransactions.isEmpty()) {
			return "Fail to " + operation + " - current transaction was already over, globalTxId [" + txEvent.globalTxId() + "].";
		}

	    // To verify if current transaction was suspended.
		boolean isPaused = txConsistentService.isGlobalTxPaused(txEvent.globalTxId());
		// The pause operation will not be executed when it is already paused.
		if (pauseOperation && isPaused) {
			return "Fail to " + operation + " - current transaction was suspended, globalTxId [" + txEvent.globalTxId() + "].";
		}
		
		// The continue operation will not be executed when it is already not paused.
		if (!pauseOperation && !isPaused){
			return "Fail to " + operation + " - current transaction was not suspended yet, globalTxId [" + txEvent.globalTxId() + "].";
		}
		
		String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
		
		eventRepository.save(new TxEvent(ip_port, ip_port, txEvent.globalTxId(), txEvent.localTxId(), txEvent.parentTxId(), additionalEventType.name(), "", pausePeriod, "", 0, null));
		
		return "ok";
	}

}
