/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@Controller
@RequestMapping("/")
class AlphaEventController {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventEnvelopeRepository eventRepository;

  AlphaEventController(TxEventEnvelopeRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @GetMapping(value = "/events")
  ResponseEntity<Collection<TxEventVo>> events() {
    LOG.info("Get the events request");
    Iterable<TxEvent> events = eventRepository.findAll();

    List<TxEventVo> eventVos = new LinkedList<>();
    events.forEach(event -> eventVos.add(new TxEventVo(event)));
    LOG.info("Get the event size " + eventVos.size());

    return ResponseEntity.ok(eventVos);
  }

  @DeleteMapping("/events")
  ResponseEntity<String> clear() {
    eventRepository.deleteAll();
    return ResponseEntity.ok("All events deleted");
  }

  @JsonAutoDetect(fieldVisibility = Visibility.ANY)
  private static final class TxEventVo extends TxEvent {
    private TxEventVo(TxEvent event) {
      super(event);
    }
  }
}
