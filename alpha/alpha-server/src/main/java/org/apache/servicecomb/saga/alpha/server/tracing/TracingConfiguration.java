package org.apache.servicecomb.saga.alpha.server.tracing;

import brave.CurrentSpanCustomizer;
import brave.SpanCustomizer;
import brave.Tracing;
import brave.handler.FinishedSpanHandler;
import brave.handler.MutableSpan;
import brave.http.HttpTracing;
import brave.httpclient.TracingHttpClientBuilder;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import brave.servlet.TracingFilter;
import brave.spring.webmvc.SpanCustomizingAsyncHandlerInterceptor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

import javax.servlet.Filter;
import java.io.IOException;

/**
 * This adds tracing configuration to any web mvc controllers or rest template clients.
 */
@Configuration
// Importing a class is effectively the same as declaring bean methods
@Import(SpanCustomizingAsyncHandlerInterceptor.class)
public class TracingConfiguration extends WebMvcConfigurerAdapter {
    @Value("${spring.zipkin.base-url://127.0.0.1:9411/api/v2/spans}")
    private String zipkinServer;

    /**
     * Configuration for how to send spans to Zipkin
     */
    @Bean
    Sender sender() {
        return OkHttpSender.create(zipkinServer);
    }

    /**
     * Configuration for how to buffer spans into messages for Zipkin
     */
    @Bean
    AsyncReporter<Span> spanReporter() {
        return AsyncReporter.create(sender());
    }

    /**
     * Controls aspects of tracing such as the service name that shows up in the UI
     */
    @Bean
    Tracing tracing(@Value("${spring.application.name}") String serviceName, Sender sender, AsyncReporter reporter) {
//        final Pattern exclude = Pattern.compile("(set.+)|(commit)|(show)|(select @@)", Pattern.CASE_INSENSITIVE);
        final String[] sqlStartArr = new String[]{"set", "commit", "/* mysql-", "show", "select @@session"};
        Tracing tracing = Tracing.newBuilder()
                .localServiceName(serviceName)
//                .propagationFactory(ExtraFieldPropagation.newFactory(B3Propagation.FACTORY, "user-name"))
                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
//                        .addScopeDecorator(MDCScopeDecorator.create()) // puts trace IDs into logs
                                .build()
                )
                .spanReporter(reporter).addFinishedSpanHandler(new FinishedSpanHandler() {
                    @Override
                    public boolean handle(TraceContext traceContext, MutableSpan mutableSpan) {
                        String tag = mutableSpan.tag("sql.query");
                        if (tag != null) {
                            tag = tag.toLowerCase();
                            if (tag.endsWith(EventScanner.SCANNER_SQL)) {
                                return false;
                            }
                            for (String start : sqlStartArr) {
                                if (tag.startsWith(start)) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                }).build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tracing.close();
                reporter.close();
                sender.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        return tracing;
    }

    /**
     * Allows someone to add tags to a span if a trace is in progress
     */
    @Bean
    SpanCustomizer spanCustomizer(Tracing tracing) {
        return CurrentSpanCustomizer.create(tracing);
    }

    /**
     * Decides how to name and tag spans. By default they are named the same as the http method
     */
    @Bean
    HttpTracing httpTracing(Tracing tracing) {
        return HttpTracing.create(tracing);
    }

    /**
     * Creates server spans for http requests
     */
    @Bean
    Filter tracingFilter(HttpTracing httpTracing) {
        return TracingFilter.create(httpTracing);
    }

    @Bean
    RestTemplateCustomizer useTracedHttpClient(HttpTracing httpTracing) {
        final CloseableHttpClient httpClient = TracingHttpClientBuilder.create(httpTracing).build();
        return restTemplate -> restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Autowired
    SpanCustomizingAsyncHandlerInterceptor webMvcTracingCustomizer;

    /**
     * Decorates server spans with application-defined web tags
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webMvcTracingCustomizer);
    }
}