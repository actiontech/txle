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

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;

import java.util.Date;

/**
 * Kafka message.
 *
 * @author Gannalyo
 * @since 2018/12/3
 */
public class KafkaMessage {
    private Date createtime;
    // 0-init, 1-sending, 2-success, 3-fail
    private int status;
    private int version;

    // message body, datasource, table and data and the like, from business.
    private String dbdrivername;
    private String dburl;
    private String dbusername;

    private String tablename;
    private String operation;
    private String ids;

    private String globaltxid;
    private String localtxid;

    private KafkaMessage() {
    }

    public KafkaMessage(String dbdrivername, String dburl, String dbusername, String tablename, String operation, String ids) {
        this.status = 0;
        this.createtime = new Date(System.currentTimeMillis());
        this.version = 1;
        this.globaltxid = CurrentThreadOmegaContext.getGlobalTxIdFromCurThread();
        this.localtxid = CurrentThreadOmegaContext.getLocalTxIdFromCurThread();
        this.dbdrivername = dbdrivername;
        this.dburl = dburl;
        this.dbusername = dbusername;
        this.tablename = tablename;
        this.operation = operation;
        this.ids = ids;
    }

    public KafkaMessage(String globaltxid, String localtxid, String dbdrivername, String dburl, String dbusername, String tablename, String operation, String ids) {
        this.status = 0;
        this.createtime = new Date(System.currentTimeMillis());
        this.version = 1;
        this.globaltxid = globaltxid;
        this.localtxid = localtxid;
        this.dbdrivername = dbdrivername;
        this.dburl = dburl;
        this.dbusername = dbusername;
        this.tablename = tablename;
        this.operation = operation;
        this.ids = ids;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public int getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    public String getDburl() {
        return dburl;
    }

    public String getDbdrivername() {
        return dbdrivername;
    }

    public String getDbusername() {
        return dbusername;
    }

    public String getTablename() {
        return tablename;
    }

    public String getOperation() {
        return operation;
    }

    public String getIds() {
        return ids;
    }

    public String getGlobaltxid() {
        return globaltxid;
    }

    public String getLocaltxid() {
        return localtxid;
    }
}
