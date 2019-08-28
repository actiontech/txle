/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Collections.singletonList;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(AlphaEventController.class)
public class AlphaEventControllerTest {
  private final TxEvent someEvent = someEvent();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TxEventEnvelopeRepository eventRepository;

  @Before
  public void setUp() throws Exception {
    when(eventRepository.findAll()).thenReturn(singletonList(someEvent));
  }

  @Test
  public void retrievesEventsFromRepo() throws Exception {
    mockMvc.perform(get("/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$.[0].globalTxId", is(someEvent.globalTxId())))
        .andExpect(jsonPath("$.[0].localTxId", is(someEvent.localTxId())));
  }

  private TxEvent someEvent() {
    return new TxEvent(
        uniquify("serviceName"),
        uniquify("instanceId"),
        uniquify("globalTxId"),
        uniquify("localTxId"),
        UUID.randomUUID().toString(),
        TxStartedEvent.name(),
        this.getClass().getCanonicalName(),
        "",
        uniquify("blah").getBytes());
  }
}
