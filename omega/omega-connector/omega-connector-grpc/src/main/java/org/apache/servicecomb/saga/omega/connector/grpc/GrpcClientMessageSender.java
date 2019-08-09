/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.connector.grpc.LoadBalancedClusterMessageSender.ErrorHandlerFactory;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.context.TxleStaticConfig;
import org.apache.servicecomb.saga.omega.transaction.*;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandling;
import org.apache.servicecomb.saga.pack.contract.grpc.*;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent.Builder;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceBlockingStub;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceStub;

import java.util.HashSet;
import java.util.Set;

public class GrpcClientMessageSender implements MessageSender {
  private final String target;
  private final TxEventServiceStub asyncEventService;

  private final MessageSerializer serializer;
  private final MessageDeserializer deserializer;

  private final TxEventServiceBlockingStub blockingEventService;

  private final GrpcCompensateStreamObserver compensateStreamObserver;
  private final GrpcServiceConfig serviceConfig;

  public GrpcClientMessageSender(
      String address,
      ManagedChannel channel,
      MessageSerializer serializer,
      MessageDeserializer deserializer,
      ServiceConfig serviceConfig,
      ErrorHandlerFactory errorHandlerFactory,
      MessageHandler handler) {
    this.target = address;
    this.asyncEventService = TxEventServiceGrpc.newStub(channel);
    this.blockingEventService = TxEventServiceGrpc.newBlockingStub(channel);//.withDeadlineAfter(20, TimeUnit.SECONDS); // 这个应该是GRPC客户端和服务端的连接超时设置，如果设置的话，一旦超时则整个连接将不可用，应给通道内的每个请求设置超时时间
    this.serializer = serializer;
    this.deserializer = deserializer;

    this.compensateStreamObserver =
        new GrpcCompensateStreamObserver(handler, errorHandlerFactory.getHandler(this), deserializer);
    this.serviceConfig = serviceConfig(serviceConfig.serviceName(), serviceConfig.instanceId(), "");
  }

  @Override
  public void onConnected() {
    asyncEventService.onConnected(serviceConfig, compensateStreamObserver);
  }

  @Override
  public void onDisconnected() {
    blockingEventService.onDisconnected(serviceConfig);
  }

  @Override
  public void close() {
    // just do nothing here
  }

  @Override
  public String target() {
    return target;
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    try {
      // To set serviceName to OmegaContextServiceConfig.
      OmegaContextServiceConfig context = CurrentThreadOmegaContext.getContextFromCurThread();
      if (context != null) {
        context.setServiceName(serviceConfig.getServiceName());
        context.setInstanceId(serviceConfig.getInstanceId());
      }
    } catch (Exception e) {}

//    blockingEventService.withDeadlineAfter(5, TimeUnit.SECONDS);// TODO 设置本次通信的超时时间
    GrpcAck grpcAck = blockingEventService.onTxEvent(convertEvent(event));
	while (grpcAck.getPaused()) {// It's a manual operation to pause transaction, so it can accept to pause for one minute.
      try {Thread.sleep(TxleStaticConfig.getIntegerConfig("txle.transaction.pause-check-interval", 60) * 1000);} catch (InterruptedException e) {}
		grpcAck = blockingEventService.onTxEvent(convertEvent(event));
		if (!grpcAck.getPaused()) {
			break;
		}
	}

    return new AlphaResponse(grpcAck.getAborted(), grpcAck.getPaused(), grpcAck.getIsEnabledTx());// To append the pause status for global transaction By Gannalyo
  }

  @Override
  public Set<String> send(Set<String> localTxIdSet) {
    ByteString payloads = ByteString.copyFrom(serializer.serialize(localTxIdSet.toArray()));

    Builder builder = GrpcTxEvent.newBuilder().setCategory(TxleConstants.SPECIAL_KEY).setPayloads(payloads);
    GrpcTxEvent grpcTxEvent = builder.build();
    blockingEventService.onTxEvent(grpcTxEvent);

    Object[] deserializerPayloads = deserializer.deserialize(grpcTxEvent.getPayloads().toByteArray());
    if (deserializerPayloads != null && deserializerPayloads.length > 0) {
      Set<String> localTxIdSetOfEndedGlobalTx = new HashSet<>();
      for (Object obj : deserializerPayloads) {
        localTxIdSetOfEndedGlobalTx.add(obj + "");
      }
      return localTxIdSetOfEndedGlobalTx;
    }
    return null;
  }

  @Override
  public String reportMessageToServer(KafkaMessage message) {
    GrpcMessage grpcMessage = GrpcMessage.newBuilder()
            .setCreatetime(message.getCreatetime().getTime())
            .setStatus(message.getStatus())
            .setVersion(message.getVersion())
            .setDbdrivername(message.getDbdrivername())
            .setDburl(message.getDburl())
            .setDbusername(message.getDbusername())
            .setTablename(message.getTablename())
            .setOperation(message.getOperation())
            .setIds(message.getIds())
            .setGlobaltxid(message.getGlobaltxid())
            .setLocaltxid(message.getLocaltxid())
            .build();
    return blockingEventService.onMessage(grpcMessage).getStatus() + "";
  }

  @Override
  public String reportAccidentToServer(AccidentHandling accident) {
    GrpcAccident grpcAccident = GrpcAccident.newBuilder()
            .setServicename(accident.getServicename())
            .setInstanceid(accident.getInstanceid())
            .setGlobaltxid(accident.getGlobaltxid())
            .setLocaltxid(accident.getLocaltxid())
            .setType(accident.getType())
            .setBizinfo(accident.getBizinfo())
            .setRemark(accident.getRemark())
            .build();
    return blockingEventService.onAccident(grpcAccident).getStatus() + "";
  }

  @Override
  public GrpcConfigAck readConfigFromServer(int type, String category) {
    if (category == null) category = "";
    return blockingEventService.onReadConfig(GrpcConfig.newBuilder().setInstanceId(serviceConfig.getInstanceId()).setServiceName(serviceConfig.getServiceName()).setCategory(category).setType(type).build());
  }

  private GrpcTxEvent convertEvent(TxEvent event) {
    ByteString payloads = ByteString.copyFrom(serializer.serialize(event.payloads()));

    Builder builder = GrpcTxEvent.newBuilder()
        .setServiceName(serviceConfig.getServiceName())
        .setInstanceId(serviceConfig.getInstanceId())
        .setTimestamp(event.timestamp())
        .setGlobalTxId(event.globalTxId())
        .setLocalTxId(event.localTxId())
        .setParentTxId(event.parentTxId() == null ? "" : event.parentTxId())
        .setType(event.type().name())
        .setTimeout(event.timeout())
        .setCompensationMethod(event.compensationMethod())
        .setRetryMethod(event.retryMethod() == null ? "" : event.retryMethod())
        .setRetries(event.retries())
        .setCategory(event.category())
        .setPayloads(payloads);

    return builder.build();
  }

  private GrpcServiceConfig serviceConfig(String serviceName, String instanceId, String category) {
    return GrpcServiceConfig.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setCategory(category)
        .build();
  }
}
