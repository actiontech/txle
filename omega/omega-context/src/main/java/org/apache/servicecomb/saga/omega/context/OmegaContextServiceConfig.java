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
 *
 *
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

/**
 * OmegaContext holds the globalTxId and localTxId which are used to build the invocation map
 */
public class OmegaContextServiceConfig extends OmegaContext {
    // Do not need think about concurrency situation, due to they're one-to-one with current application.
    private String serviceName;
    private String instanceId;
    // compensation or auto-compensation
    private boolean isAutoCompensate;
    // true: record undo_log, otherwise do nothing, just for auto-compensation.
    private boolean isEnabledAutoCompensateTx;

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String serviceName() {
        return serviceName;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String instanceId() {
        return instanceId;
    }

    public boolean isAutoCompensate() {
        return isAutoCompensate;
    }

    public void setAutoCompensate(boolean autoCompensate) {
        this.isAutoCompensate = autoCompensate;
    }

    public boolean isEnabledAutoCompensateTx() {
        return this.isEnabledAutoCompensateTx;
    }

    public void setIsEnabledAutoCompensateTx(boolean isEnabledAutoCompensateTx) {
        this.isEnabledAutoCompensateTx = isEnabledAutoCompensateTx;
    }

    public OmegaContextServiceConfig(IdGenerator<String> idGenerator) {
        super(idGenerator);
    }

    public OmegaContextServiceConfig(OmegaContext context) {
        super(null);
        this.setGlobalTxId(context.globalTxId());
        this.setLocalTxId(context.localTxId());
        this.setCategory(context.category());
    }

    public OmegaContextServiceConfig(OmegaContext context, boolean isAutoCompensate, boolean isEnabledAutoCompensateTx) {
        super(null);
        this.setGlobalTxId(context.globalTxId());
        this.setLocalTxId(context.localTxId());
        this.setCategory(context.category());
        this.setAutoCompensate(isAutoCompensate);
        this.setIsEnabledAutoCompensateTx(isEnabledAutoCompensateTx);
    }

}
