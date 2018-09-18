package org.apache.servicecomb.saga.alpha.core;

public enum AdditionalEventType {
	// It's a pity it could not extend the enum 'EventType'.
	SagaPausedEvent,
	SagaContinuedEvent,
	SagaAutoContinuedEvent
}
