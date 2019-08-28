/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.*;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandling;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcConfigAck;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CompensableInterceptorTest {

  private final List<TxEvent> messages = new ArrayList<>();
  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();

  private final MessageSender sender =new MessageSender() {
    @Override
    public void onConnected() {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void close() {
    }

    @Override
    public String target() {
      return "UNKNOWN";
    }

    @Override
    public AlphaResponse send(TxEvent event) {
      messages.add(event);
      return new AlphaResponse(false);
    }

    @Override
    public Set<String> send(Set<String> localTxIdSet) {
      return null;
    }

    @Override
    public String reportMessageToServer(KafkaMessage message) {
      return "";
    }

    @Override
    public String reportAccidentToServer(AccidentHandling accidentHandling) {
      return null;
    }

    @Override
    public GrpcConfigAck readConfigFromServer(int type, String category) {
      return null;
    }
  };
  
  private final String message = uniquify("message");

  private final String retryMethod = uniquify("retryMethod");
  private final String compensationMethod = getClass().getCanonicalName();

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = Mockito.mock(IdGenerator.class);
  private final OmegaContext context = new OmegaContext(idGenerator);
  private final CompensableInterceptor interceptor = new CompensableInterceptor(context, sender);

  @Before
  public void setUp() throws Exception {
    context.setGlobalTxId(globalTxId);
    context.setLocalTxId(localTxId);
  }

  @Test
  public void sendsTxStartedEventBefore() throws Exception {
    int retries = new Random().nextInt();
    interceptor.preIntercept(parentTxId, compensationMethod, 0, retryMethod, retries, message);

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.retries(), is(retries));
    assertThat(event.retryMethod(), is(retryMethod));
    assertThat(event.type(), is(EventType.TxStartedEvent));
    assertThat(event.compensationMethod(), is(compensationMethod));
    assertThat(asList(event.payloads()).contains(message), is(true));
  }

  @Test
  public void sendsTxEndedEventAfter() throws Exception {
    interceptor.postIntercept(parentTxId, compensationMethod);

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxEndedEvent));
    assertThat(event.compensationMethod(), is(compensationMethod));
    assertThat(event.payloads().length, is(0));
  }

  @Test
  public void sendsTxAbortedEventOnError() throws Exception {
    interceptor.onError(parentTxId, compensationMethod, new RuntimeException("oops"));

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxAbortedEvent));
    assertThat(event.compensationMethod(), is(compensationMethod));
  }
}
