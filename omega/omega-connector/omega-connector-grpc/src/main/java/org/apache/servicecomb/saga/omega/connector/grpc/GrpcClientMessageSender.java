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

import org.apache.servicecomb.saga.omega.connector.grpc.LoadBalancedClusterMessageSender.ErrorHandlerFactory;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent.Builder;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceBlockingStub;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceStub;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;

public class GrpcClientMessageSender implements MessageSender {
  private final String target;
  private final TxEventServiceStub asyncEventService;

  private final MessageSerializer serializer;

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
    this.blockingEventService = TxEventServiceGrpc.newBlockingStub(channel);
    this.serializer = serializer;

    this.compensateStreamObserver =
        new GrpcCompensateStreamObserver(handler, errorHandlerFactory.getHandler(this), deserializer);
    this.serviceConfig = serviceConfig(serviceConfig.serviceName(), serviceConfig.instanceId());
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

    GrpcAck grpcAck = blockingEventService.onTxEvent(convertEvent(event));
    // TODO any hidden trouble about current logic???
    // If transaction is paused, then Client will retry.
//    try {Thread.sleep(10 * 1000);} catch (InterruptedException e) {}
	while (grpcAck.getPaused()) {
		// TODO default 60s, support to configure in the future.
		try {Thread.sleep(10 * 1000);} catch (InterruptedException e) {}
		grpcAck = blockingEventService.onTxEvent(convertEvent(event));
		if (!grpcAck.getPaused()) {
			break;
		}
	}
    
    return new AlphaResponse(grpcAck.getAborted(), grpcAck.getPaused());// To append the pause status for global transaction By Gannalyo
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

  private GrpcServiceConfig serviceConfig(String serviceName, String instanceId) {
    return GrpcServiceConfig.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .build();
  }
}
