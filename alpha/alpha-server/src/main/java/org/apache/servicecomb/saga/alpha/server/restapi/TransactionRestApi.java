package org.apache.servicecomb.saga.alpha.server.restapi;

import org.apache.servicecomb.saga.alpha.core.*;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;

@RestController
public class TransactionRestApi {
	private static final Logger LOG = LoggerFactory.getLogger(TransactionRestApi.class);

	@Autowired
	private TxEventRepository eventRepository;
	
	private TxConsistentService txConsistentService;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	UtxMetrics utxMetrics;
	
	public TransactionRestApi(TxConsistentService txConsistentService) {
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
	 * @param txEventId identify for global transaction
	 * @return
	 */
	@GetMapping("/pauseGlobalTransactionById/{txEventId}")
	public String pauseGlobalTransactionById(@PathVariable long txEventId) {
		return pauseGlobalTransactionById(txEventId, 0);
	}

	/**
	 * To pause specified global transaction by identify.
	 * 
	 * @param txEventId identify for global transaction
	 * @param pausePeriod The global transaction will continue to run in 'pausePeriod' seconds automatically. It will not continue to if the value of its pausePeriod is 0. The former is the paused must be true;
	 * @return
	 */
	@GetMapping("/pauseGlobalTransactionById/{txEventId}/{pausePeriod}")
	public String pauseGlobalTransactionById(@PathVariable long txEventId, @PathVariable int pausePeriod) {
		if (pausePeriod < 0) {
			pausePeriod = 0;
		}
		String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
		LOG.debug("Executing the 'pauseGlobalTransactionById' API - txEventId [{}], pausePeriod [{}], ip:port [{}].", txEventId, pausePeriod, ip_port);
		
		return saveOperationTxEventWithVerification(txEventId, pausePeriod, 0, "pause", ip_port);
	}

	@GetMapping("/continueGlobalTransactionById/{txEventId}")
	public String continueGlobalTransactionById(@PathVariable long txEventId) {
		String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
		LOG.debug("Executing the 'continueGlobalTransactionById' API - txEventId [{}], ip:port [{}].", txEventId, ip_port);
		
		return saveOperationTxEventWithVerification(txEventId, 0, 1, "continue", ip_port);
	}
	
	@GetMapping("/terminateGlobalTransactionById/{txEventId}")
	public String terminateGlobalTransactionById(@PathVariable long txEventId) {
		String ip_port = request.getRemoteAddr() + ":" + request.getRemotePort();
		LOG.debug("Executing the 'terminateGlobalTransactionById' API - txEventId [{}], ip:port [{}].", txEventId, ip_port);
		
		return saveOperationTxEventWithVerification(txEventId, 0, -1, "terminate", ip_port);
	}
	
	private String saveOperationTxEventWithVerification(long txEventId, int pausePeriod, int operation, String operationDesc, String ip_port) {
		TxEvent txEvent = eventRepository.findOne(txEventId);
		if (txEvent == null) {
			return "Fail to " + operationDesc + " - could not find transaction by txEventId [" + txEventId + "].";
		}
		String globalTxId = txEvent.globalTxId();

		// To verify if current transaction had been over.
		List<TxEvent> endedTransactions = eventRepository.findTransactions(txEvent.globalTxId(),
				EventType.SagaEndedEvent.name());
		if (endedTransactions != null && !endedTransactions.isEmpty()) {
			return "Fail to " + operationDesc + " - current transaction was already over, globalTxId [" + globalTxId + "].";
		}

		String eventTypeName = null;
		// 0 - pause, 1 - continue, -1 - terminate
		switch (operation) {
			case 0:
				// To verify if current transaction was suspended.
				boolean isPaused = txConsistentService.isGlobalTxPaused(globalTxId, txEvent.type());
				// The pause operation will not be executed when it is already paused.
				if (isPaused) {
					return "Fail to " + operation + " - current transaction was suspended, globalTxId [" + globalTxId + "].";
				}
				eventTypeName = AdditionalEventType.SagaPausedEvent.name();
				break;
			case 1:
				// To verify if current transaction was suspended.
				isPaused = txConsistentService.isGlobalTxPaused(globalTxId, txEvent.type());
				// The continue operation will not be executed when it is already not paused.
				if (!isPaused) {
					return "Fail to " + operation + " - current transaction was not suspended yet, globalTxId [" + globalTxId + "].";
				}
				eventTypeName = AdditionalEventType.SagaContinuedEvent.name();
				break;
			case -1:
				List<TxEvent> abortedTxEventList = eventRepository.findTransactions(globalTxId, TxAbortedEvent.name());
				if (abortedTxEventList != null && !abortedTxEventList.isEmpty()) {
					return "Fail to " + operationDesc + " - current transaction was already aborted, globalTxId [" + globalTxId + "].";
				}
				eventTypeName = EventType.TxAbortedEvent.name();
				break;
			default:
				break;
		}

		TxEvent event = new TxEvent(ip_port, ip_port, globalTxId, txEvent.localTxId(), txEvent.parentTxId(), eventTypeName, "", pausePeriod, "", 0, txEvent.category(), null);
		eventRepository.save(event);
		utxMetrics.countTxNumber(event, false, false);

		return "ok";
	}

}
