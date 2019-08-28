/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.resttemplate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.HandlerInterceptor;

public class TransactionHandlerInterceptorTest {

  private static final String globalTxId = UUID.randomUUID().toString();

  private static final String localTxId = UUID.randomUUID().toString();

  private final OmegaContext omegaContext = new OmegaContext(new IdGenerator<String>() {

    @Override
    public String nextId() {
      return "ignored";
    }
  });

  private HandlerInterceptor requestInterceptor = new TransactionHandlerInterceptor(omegaContext);

  private HttpServletRequest request = mock(HttpServletRequest.class);

  private HttpServletResponse response = mock(HttpServletResponse.class);

  @Before
  public void setUp() {
    omegaContext.clear();
  }

  @Test
  public void setUpOmegaContextInTransactionRequest() throws Exception {
    when(request.getHeader(OmegaContext.GLOBAL_TX_ID_KEY)).thenReturn(globalTxId);
    when(request.getHeader(OmegaContext.LOCAL_TX_ID_KEY)).thenReturn(localTxId);

    requestInterceptor.preHandle(request, response, null);

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void doNothingInNonTransactionRequest() throws Exception {
    when(request.getHeader(OmegaContext.GLOBAL_TX_ID_KEY)).thenReturn(null);
    when(request.getHeader(OmegaContext.LOCAL_TX_ID_KEY)).thenReturn(null);

    requestInterceptor.preHandle(request, response, null);

    assertThat(omegaContext.globalTxId(), is(nullValue()));
    assertThat(omegaContext.localTxId(), is(nullValue()));
  }
}
