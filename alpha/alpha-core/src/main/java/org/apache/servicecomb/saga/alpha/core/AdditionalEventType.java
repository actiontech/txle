/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

public enum AdditionalEventType {
	// It's a pity it could not extend the enum 'EventType'.
	SagaPausedEvent,
	SagaContinuedEvent,
	SagaAutoContinuedEvent
}
