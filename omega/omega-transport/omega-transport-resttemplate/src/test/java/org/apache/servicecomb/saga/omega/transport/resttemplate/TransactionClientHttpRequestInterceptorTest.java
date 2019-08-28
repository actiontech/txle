/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.resttemplate;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class TransactionClientHttpRequestInterceptorTest {

  private final HttpRequest request = mock(HttpRequest.class);

  private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

  private final ClientHttpResponse response = mock(ClientHttpResponse.class);

  private final String globalTxId = uniquify("global tx id");
  private final String localTxId = uniquify("local tx id");
  private final IdGenerator<String> idGenerator = Mockito.mock(IdGenerator.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);
  private final ClientHttpRequestInterceptor clientHttpRequestInterceptor = new TransactionClientHttpRequestInterceptor(
      omegaContext
  );

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(globalTxId, localTxId);
  }

  @Test
  public void keepHeaderUnchangedIfContextAbsent() throws IOException {
    when(request.getHeaders()).thenReturn(new HttpHeaders());

    when(execution.execute(request, null)).thenReturn(response);

    clientHttpRequestInterceptor.intercept(request, null, execution);

    assertThat(request.getHeaders().isEmpty(), is(true));
  }

  @Test
  public void interceptTransactionIdInHeaderIfContextPresent() throws IOException {
    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(localTxId);

    HttpHeaders headers = new HttpHeaders();
    when(request.getHeaders()).thenReturn(headers);
    when(execution.execute(request, null)).thenReturn(response);

    clientHttpRequestInterceptor.intercept(request, null, execution);

    assertThat(request.getHeaders().get(OmegaContext.GLOBAL_TX_ID_KEY), contains(globalTxId));
    assertThat(request.getHeaders().get(OmegaContext.LOCAL_TX_ID_KEY), contains(localTxId));
  }
}
