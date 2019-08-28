/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.feign;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.UUID;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class FeignAutoConfigurationTest{

    private static final String globalTxId = UUID.randomUUID().toString();

    private static final String localTxId = UUID.randomUUID().toString();

    private final OmegaContext omegaContext = new OmegaContext(new IdGenerator<String>() {

        @Override
        public String nextId() {
            return "ignored";
        }
    });

    private RequestTemplate requestTemplate = new RequestTemplate(); // mock(RequestTemplate.class);

    private final RequestInterceptor feignClientRequestInterceptor = new FeignClientRequestInterceptor(omegaContext);

    @Before
    public void setUp() {
        omegaContext.clear();
    }

    @Test
    public void setUpOmegaContextInTransactionRequest() throws Exception {
        omegaContext.setGlobalTxId(globalTxId);
        omegaContext.setLocalTxId(localTxId);

        feignClientRequestInterceptor.apply(requestTemplate); // .preHandle(request, response, null);

        assertThat((new ArrayList(requestTemplate.headers().get(OmegaContext.GLOBAL_TX_ID_KEY))).get(0),
            Matchers.<Object>is(globalTxId));
        assertThat((new ArrayList(requestTemplate.headers().get(OmegaContext.LOCAL_TX_ID_KEY))).get(0),
            Matchers.<Object>is(localTxId));
    }

    @Test
    public void doNothingInNonTransactionRequest() throws Exception {
        omegaContext.setGlobalTxId(null);
        omegaContext.setLocalTxId(null);

        feignClientRequestInterceptor.apply(requestTemplate);

        assertThat(requestTemplate.headers().get(OmegaContext.GLOBAL_TX_ID_KEY), is(nullValue()));
        assertThat(requestTemplate.headers().get(OmegaContext.LOCAL_TX_ID_KEY), is(nullValue()));
    }
}
