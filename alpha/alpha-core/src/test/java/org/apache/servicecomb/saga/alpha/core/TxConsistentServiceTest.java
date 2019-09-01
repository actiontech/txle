/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import org.apache.servicecomb.saga.common.EventType;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Collections.emptyList;
import static org.apache.servicecomb.saga.common.EventType.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TxConsistentServiceTest {
  private final Deque<TxEvent> events = new ConcurrentLinkedDeque<>();
  private final TxEventRepository eventRepository = new TxEventRepository() {
    @Override
    public void save(TxEvent event) {
      events.add(event);
    }

    @Override
    public List<TxEvent> findTimeoutEvents(long unendedMinEventId) {
      return emptyList();
    }

    @Override
    public TxEvent findTimeoutEventsBeforeEnding(String globalTxId) {
      return null;
    }

    @Override
    public Optional<TxEvent> findTxStartedEvent(String globalTxId, String localTxId) {
      return events.stream()
          .filter(event -> globalTxId.equals(event.globalTxId()) && localTxId.equals(event.localTxId()))
          .findFirst();
    }

    @Override
    public List<TxEvent> findSequentialCompensableEventOfUnended(long unendedMinEventId) {
      return null;
    }

    @Override
    public List<String> selectAllTypeByGlobalTxId(String globalTxId) {
      return null;
    }

    @Override
	public List<TxEvent> selectPausedAndContinueEvent(String globalTxId) {
		return null;
	}

    @Override
    public Set<String> selectEndedGlobalTx(Set<String> localTxIdSet) {
      return null;
    }

    @Override
    public boolean checkIsExistsEventType(String globalTxId, String localTxId, String type) {
      return false;
    }

    @Override
    public boolean checkTxIsAborted(String globalTxId, String localTxId) {
      return false;
    }

    @Override
    public List<Map<String, Object>> findTxList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
      return null;
    }

    @Override
    public List<TxEvent> selectTxEventByGlobalTxIds(List<String> globalTxIdList) {
      return null;
    }

    @Override
    public long findTxCount(String searchText) {
      return 0;
    }

    @Override
    public List<Map<String, Object>> findSubTxList(String globalTxIds) {
      return null;
    }

    @Override
    public List<TxEvent> selectUnendedTxEvents(long unendedMinEventId) {
      return null;
    }

    @Override
    public long selectMinUnendedTxEventId(long unendedMinEventId) {
      return 0;
    }

    @Override
    public Date selectMinDateInTxEvent() {
      return null;
    }

    @Override
    public List<Long> selectEndedEventIdsWithinSomePeriod(int pageIndex, int pageSize, Date startTime, Date endTime) {
      return null;
    }

    @Override
    public TxEvent selectEventByGlobalTxIdType(String globalTxId, String type) {
      return null;
    }

    @Override
    public long selectSubTxCount(String globalTxId) {
      return 0;
    }
  };

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final String compensationMethod = getClass().getCanonicalName();

  private final TxConsistentService consistentService = new TxConsistentService(eventRepository, null, null);
  private final byte[] payloads = "yeah".getBytes();

  @Before
  public void setUp() throws Exception {
    events.clear();
  }

  @Test
  public void persistEventOnArrival() throws Exception {
    TxEvent[] events = {
        newEvent(SagaStartedEvent),
        newEvent(TxStartedEvent),
        newEvent(TxEndedEvent),
        newEvent(TxCompensatedEvent),
        newEvent(SagaEndedEvent)};

    for (TxEvent event : events) {
      consistentService.handle(event);
    }

    assertThat(this.events, contains(events));
  }

  @Test
  public void skipTxStartedEvent_IfGlobalTxAlreadyFailed() {
    String localTxId1 = UUID.randomUUID().toString();
    events.add(newEvent(TxStartedEvent));
    events.add(newEvent(TxAbortedEvent));

    TxEvent event = eventOf(TxStartedEvent, localTxId1);

    consistentService.handle(event);

    assertThat(events.size(), is(2));
  }

  @Test
  public void skipSagaEndedEvent_IfGlobalTxAlreadyFailed() {
    String localTxId1 = UUID.randomUUID().toString();
    events.add(eventOf(SagaStartedEvent, localTxId1));
    events.add(eventOf(TxAbortedEvent, localTxId1));

    TxEvent event = eventOf(SagaEndedEvent, localTxId1);

    consistentService.handle(event);

    assertThat(events.size(), is(2));
  }

  private TxEvent newEvent(EventType eventType) {
    return new TxEvent(serviceName, instanceId, globalTxId, localTxId, parentTxId, eventType.name(), compensationMethod, "",
        payloads);
  }

  private TxEvent eventOf(EventType eventType, String localTxId) {
    return new TxEvent(serviceName,
        instanceId,
        globalTxId,
        localTxId,
        UUID.randomUUID().toString(),
        eventType.name(),
        compensationMethod,
        "",
        payloads);
  }
}
