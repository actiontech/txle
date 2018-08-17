package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.OmegaContext;

class AutoCompensableInterceptor implements EventAwareInterceptor {
	private final OmegaContext context;
	private final MessageSender sender;

	AutoCompensableInterceptor(OmegaContext context, MessageSender sender) {
		this.sender = sender;
		this.context = context;
	}

	@Override
	public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
			int retries, Object... message) {
		return sender.send(new TxStartedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod,
				timeout, retriesMethod, retries, message));
	}

	@Override
	public void postIntercept(String parentTxId, String compensationMethod) {
		sender.send(new TxEndedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod));
	}

	@Override
	public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
		sender.send(new TxAbortedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod,
				throwable));
	}

//	public void onAutoCompensate(String parentTxId, String compensationMethod, Throwable throwable) {
//		sender.send(new TxAutoCompensatedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod));
//	}
}
