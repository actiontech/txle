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

package org.apache.servicecomb.saga.alpha.server;

import static java.util.Collections.emptyMap;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.protobuf.ByteString;
import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;

import io.grpc.stub.StreamObserver;

class GrpcTxEventEndpointImpl extends TxEventServiceImplBase {

  private static final GrpcAck ALLOW = GrpcAck.newBuilder().setAborted(false).build();
  private static final GrpcAck REJECT = GrpcAck.newBuilder().setAborted(true).build();
  private static final GrpcAck PAUSED = GrpcAck.newBuilder().setAborted(false).setPaused(true).build();

  private final KryoPool pool = new KryoPool.Builder(() -> new Kryo()).softReferences().build();

  private final TxConsistentService txConsistentService;

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  GrpcTxEventEndpointImpl(TxConsistentService txConsistentService,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks) {
    this.txConsistentService = txConsistentService;
    this.omegaCallbacks = omegaCallbacks;
  }

  @Override
  public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcCompensateCommand> responseObserver) {
    omegaCallbacks
        .computeIfAbsent(request.getServiceName(), key -> new ConcurrentHashMap<>())
        .put(request.getInstanceId(), new GrpcOmegaCallback(responseObserver));
  }

  // TODO: 2018/1/5 connect is async and disconnect is sync, meaning callback may not be registered on disconnected
  @Override
  public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
    OmegaCallback callback = omegaCallbacks.getOrDefault(request.getServiceName(), emptyMap())
        .remove(request.getInstanceId());

    if (callback != null) {
      callback.disconnect();
    }

    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }

  @Override
  public void onTxEvent(GrpcTxEvent message, StreamObserver<GrpcAck> responseObserver) {
    if ("UTX-SPECIAL-KEY".equals(message.getCategory())) {
      fetchLocalTxIdOfEndedGlobalTx(message, responseObserver);
      return;
    }

    int result = txConsistentService.handleSupportTxPause(new TxEvent(
        message.getServiceName(),
        message.getInstanceId(),
        new Date(),
        message.getGlobalTxId(),
        message.getLocalTxId(),
        message.getParentTxId().isEmpty() ? null : message.getParentTxId(),
        message.getType(),
        message.getCompensationMethod(),
        message.getTimeout(),
        message.getRetryMethod(),
        message.getRetries(),
        message.getCategory(),
        message.getPayloads().toByteArray()
    ));

    responseObserver.onNext(result == 0 ? PAUSED : result > 0 ? ALLOW : REJECT);
    responseObserver.onCompleted();
  }

  private void fetchLocalTxIdOfEndedGlobalTx(GrpcTxEvent message, StreamObserver<GrpcAck> responseObserver) {
    ByteString payloads = null;
    try {
      // Reasons for using the Kryo serialization tool are: 1.Do not change the TxEvent's structure. 2.To decrease data size for saving I/O. 3.Kryo has a high-performance computing power.
      // Disadvantages for using Kryo: Have to do serialize and deserialize, their processes will cost any performance, but just a little (very fast), and it is more cheap than I/O.
      Object[] payloadArr = deserialize(message.getPayloads().toByteArray());
      Set<String> localTxIdSet = new HashSet<>();
      for (int i = 0; i < payloadArr.length; i++) {
        localTxIdSet.add(payloadArr[i].toString());
      }
      Set<String> localTxIdOfEndedGlobalTx = txConsistentService.fetchLocalTxIdOfEndedGlobalTx(localTxIdSet);
      payloads = ByteString.copyFrom(serialize(localTxIdOfEndedGlobalTx.toArray()));
    } catch (Exception e) {
    } finally {
      // message.toBuilder().setPayloads(payloads);// Could not set payloads to the original object.
      responseObserver.onNext(GrpcAck.newBuilder().setAborted(false).setLocalTxIds(payloads).build());
      responseObserver.onCompleted();
    }
  }

  private byte[] serialize(Object[] objects) {
    Output output = new Output(4096, -1);
    Kryo kryo = pool.borrow();
    kryo.writeObjectOrNull(output, objects, Object[].class);
    pool.release(kryo);
    return output.toBytes();
  }

  private Object[] deserialize(byte[] message) {
    try {
      Input input = new Input(new ByteArrayInputStream(message));
      Kryo kryo = pool.borrow();
      Object[] objects = kryo.readObjectOrNull(input, Object[].class);
      pool.release(kryo);
      return objects;
    } catch (KryoException e) {
      throw new RuntimeException("Unable to deserialize message", e);
    }
  }
}
