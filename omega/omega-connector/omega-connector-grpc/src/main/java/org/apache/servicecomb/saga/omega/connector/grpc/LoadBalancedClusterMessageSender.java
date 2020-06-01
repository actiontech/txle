/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import brave.Tracing;
import brave.grpc.GrpcTracing;
import com.google.common.base.Supplier;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.*;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandling;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcConfigAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class LoadBalancedClusterMessageSender implements MessageSender {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<MessageSender, Long> senders = new ConcurrentHashMap<>(8);
  private final Collection<ManagedChannel> channels;

  private final BlockingQueue<Runnable> pendingTasks = new LinkedBlockingQueue<>();
  private final BlockingQueue<MessageSender> availableMessageSenders = new LinkedBlockingQueue<>();
  private final MessageSender retryableMessageSender = new RetryableMessageSender(
      availableMessageSenders);

  private final Supplier<MessageSender> defaultMessageSender = new Supplier<MessageSender>() {
    @Override
    public MessageSender get() {
      return retryableMessageSender;
    }
  };

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  public LoadBalancedClusterMessageSender(AlphaClusterConfig clusterConfig,
      MessageSerializer serializer,
      MessageDeserializer deserializer,
      ServiceConfig serviceConfig,
      MessageHandler handler,
      int reconnectDelay,
      Tracing tracing) {

    if (clusterConfig.getAddresses().size() == 0) {
      throw new IllegalArgumentException("No reachable cluster address provided");
    }

    channels = new ArrayList<>(clusterConfig.getAddresses().size());

    SslContext sslContext = null;
    for (String address : clusterConfig.getAddresses()) {
      ManagedChannel channel;

      if (clusterConfig.isEnableSSL()) {
        if (sslContext == null) {
          try {
            sslContext = buildSslContext(clusterConfig);
          } catch (SSLException e) {
            throw new IllegalArgumentException("Unable to build SslContext", e);
          }
        }
         channel = NettyChannelBuilder.forTarget(address)
            .negotiationType(NegotiationType.TLS)
            .sslContext(sslContext)
            .intercept(GrpcTracing.create(tracing).newClientInterceptor()) // add grpc interceptor for tracing By Gannalyo
            .build();
      } else {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext()
            .intercept(GrpcTracing.create(tracing).newClientInterceptor()) // add grpc interceptor for tracing By Gannalyo
            .build();
      }
      channels.add(channel);
      senders.put(
          new GrpcClientMessageSender(
              address,
              channel,
              serializer,
              deserializer,
              serviceConfig,
              new ErrorHandlerFactory(),
              handler),
          0L);
    }

    scheduleReconnectTask(reconnectDelay);
  }

  // this is for test only
  LoadBalancedClusterMessageSender(MessageSender... messageSenders) {
    for (MessageSender sender : messageSenders) {
      senders.put(sender, 0L);
    }
    channels = emptyList();
  }

  @Override
  public void onConnected() {
    for (MessageSender sender : senders.keySet()) {
      try {
        sender.onConnected();
      } catch (Exception e) {
        LOG.error("Failed connecting to alpha at {}", sender.target(), e);
      }
    }
  }

  @Override
  public void onDisconnected() {
    for (MessageSender sender :senders.keySet()) {
      try {
        sender.onDisconnected();
      } catch (Exception e) {
        LOG.error("Failed disconnecting from alpha at {}", sender.target(), e);
      }
    }
  }

  @Override
  public void close() {
    scheduler.shutdown();
    for (ManagedChannel channel : channels) {
      channel.shutdownNow();
    }
  }

  @Override
  public String target() {
    return "UNKNOWN";
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    return (AlphaResponse) send("sendEvent", event);
  }

  @Override
  public Set<String> send(Set<String> localTxIdSet) {
    return (Set<String>) send("sendLocalTxIdSet", localTxIdSet);
  }

  @Override
  public String reportMessageToServer(KafkaMessage message) {
    return (String) send("reportMessageToServer", message);
  }

  @Override
  public String reportAccidentToServer(AccidentHandling accidentHandling) {
    return (String) send("reportAccidentToServer", accidentHandling);
  }

  @Override
  public GrpcConfigAck readConfigFromServer(int type, String category) {
    return (GrpcConfigAck) send("readConfigFromServer", type, category);
  }

  private Object send(String method, Object... args) {
    FastestSender messageSenderPicker = new FastestSender();
    String errMsg = "send TxEvent" + args[0];
    do {
      MessageSender messageSender = messageSenderPicker.pick(senders, defaultMessageSender);
      Object returnObject = null;
      try {
        long startTime = System.nanoTime();
        if ("sendEvent".equals(method)) {
          returnObject = messageSender.send((TxEvent) args[0]);
        } else if ("readConfigFromServer".equals(method)) {
          errMsg = "read config, type = " + args[0] + ", category = " + args[1];
          returnObject = messageSender.readConfigFromServer(Integer.parseInt(args[0].toString()), args[1] == null ? null : args[1].toString());
        } else if ("sendLocalTxIdSet".equals(method)) {
          errMsg = "send localTxIdSet " + args[0];
          returnObject = messageSender.send((Set<String>) args[0]);
        } else if ("reportMessageToServer".equals(method)) {
          errMsg = "report message " + args[0];
          returnObject = messageSender.reportMessageToServer((KafkaMessage) args[0]);
        } else if ("reportAccidentToServer".equals(method)) {
          errMsg = "report accident " + args[0];
          returnObject = messageSender.reportAccidentToServer((AccidentHandling) args[0]);
        }
        senders.put(messageSender, System.nanoTime() - startTime);
        return returnObject;
      } catch (OmegaException e) {
        LOG.error("Failed to " + errMsg + ", messageSender = " + messageSender, e);
        throw e;
      } catch (Exception e) {
        LOG.error("Try to " + errMsg + " again due to failure", e);
        senders.put(messageSender, Long.MAX_VALUE);
      }
    } while (!Thread.currentThread().isInterrupted());

    throw new OmegaException("Failed to " + errMsg + " due to interruption.");
  }

  private void scheduleReconnectTask(int reconnectDelay) {
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          pendingTasks.take().run();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }, 0, reconnectDelay, MILLISECONDS);
  }

  /**
   * 当发生网络异常时
   * 1.尝试重连
   * 2.添加尝试重连的任务到队列中
   * 3.定时器定时检测是否重连成功，如果重连成功则添加至availableMessageSender中，可供后续send方法内再次使用
   * 4.如果重连失败，则再添加回到任务队列中，后续继续尝试重连
   */
  class ErrorHandlerFactory {
    Runnable getHandler(MessageSender messageSender) {
      final Runnable runnable = new PushBackReconnectRunnable(messageSender, senders, pendingTasks,
          availableMessageSenders);
      return new Runnable() {
        @Override
        public void run() {
          pendingTasks.offer(runnable);
        }
      };
    }

  }

  private static SslContext buildSslContext(AlphaClusterConfig clusterConfig) throws SSLException {
    SslContextBuilder builder = GrpcSslContexts.forClient();
    // openssl must be used because some older JDk does not support cipher suites required by http2,
    // and the performance of JDK ssl is pretty low compared to openssl.
    builder.sslProvider(SslProvider.OPENSSL);

    Properties prop = new Properties();
    try {
      prop.load(LoadBalancedClusterMessageSender.class.getClassLoader().getResourceAsStream("ssl.properties"));
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read ssl.properties.", e);
    }

    builder.protocols(prop.getProperty("protocols").split(","));
    builder.ciphers(Arrays.asList(prop.getProperty("ciphers").split(",")));
    builder.trustManager(new File(clusterConfig.getCertChain()));

    if (clusterConfig.isEnableMutualAuth()) {
      builder.keyManager(new File(clusterConfig.getCert()), new File(clusterConfig.getKey()));
    }

    return builder.build();
  }
}

/**
 * The strategy of picking the fastest {@link MessageSender}
 */
class FastestSender implements MessageSenderPicker {

  @Override
  public MessageSender pick(Map<MessageSender, Long> messageSenders,
      Supplier<MessageSender> defaultSender) {
    Long min = Long.MAX_VALUE;
    MessageSender sender = null;
    for (Map.Entry<MessageSender, Long> entry : messageSenders.entrySet()) {
      if (entry.getValue() != Long.MAX_VALUE) {
        if (min > entry.getValue()) {
          min = entry.getValue();
          sender = entry.getKey();
        }
      }
    }
    if (sender == null) {
//      return defaultSender.get();
      return messageSenders.keySet().iterator().next();
    } else {
      return sender;
    }
  }
}