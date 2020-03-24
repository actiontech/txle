/**
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
 */

CREATE TABLE IF NOT EXISTS TxEvent (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  serviceName varchar(100) NOT NULL,
  instanceId varchar(100) NOT NULL,
  creationTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  type varchar(50) NOT NULL,
  compensationMethod varchar(256) NULL,
  expiryTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  payloads blob,
  retries int(11) NOT NULL DEFAULT '0',
  retryMethod varchar(256) DEFAULT NULL,
  category varchar(100),
  PRIMARY KEY (surrogateId),
  INDEX saga_events_index (surrogateId, globalTxId, localTxId, type, expiryTime),
  INDEX saga_global_tx_index (globalTxId),
  INDEX saga_globalid_localid_type (globalTxId, localTxId, type),
  INDEX saga_surrogateId_index (surrogateId),
  INDEX saga_tx_type_index (type)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS TxEventAddition (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  serviceName varchar(100) NOT NULL,
  instanceId varchar(100) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  dbNodeId varchar(50) NOT NULL,
  dbSchema varchar(50) NOT NULL,
  businessSql varchar(2000) NOT NULL,
  backupSql varchar(3000) NOT NULL,
  compensateSql varchar(2000) NOT NULL,
  compensateStatus int(1) NOT NULL DEFAULT 0 COMMENT '0-uncompensated, 1-compensated',
  executeOrder int(2) NOT NULL DEFAULT 0,
  creationTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (surrogateId),
  INDEX saga_global_tx_index (globalTxId),
  INDEX saga_surrogateId_index (surrogateId)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS Command (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  eventId bigint NOT NULL UNIQUE,
  serviceName varchar(100) NOT NULL,
  instanceId varchar(100) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  compensationMethod varchar(256) NULL,
  payloads blob,
  status varchar(12),
  lastModified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version bigint NOT NULL,
  category varchar(100),
  PRIMARY KEY (surrogateId),
  INDEX saga_commands_index (surrogateId, eventId, globalTxId, localTxId, status)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS TxTimeout (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  eventId bigint NOT NULL UNIQUE,
  serviceName varchar(100) NOT NULL,
  instanceId varchar(100) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  type varchar(50) NOT NULL,
  expiryTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status varchar(12),
  version bigint NOT NULL,
  category varchar(100),
  PRIMARY KEY (surrogateId),
  INDEX saga_timeouts_index (surrogateId, expiryTime, globalTxId, localTxId, status)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS Message (
  id bigint NOT NULL AUTO_INCREMENT,
  globaltxid varchar(36) NOT NULL,
  localtxid varchar(36) NOT NULL,
  status int(1) NOT NULL DEFAULT 0 COMMENT '0-init, 1-sending, 2-success, 3-fail',
  version int(2) NOT NULL DEFAULT 1,
  dbdrivername varchar(100),
  dburl varchar(150),
  dbusername varchar(20),
  tablename varchar(255),
  operation varchar(20) DEFAULT 'update',
  ids blob,
  createtime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id) USING BTREE,
  UNIQUE INDEX pk_id(id) USING BTREE,
  INDEX txle_globalTxId_index(globaltxid) USING BTREE
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS Config (
  id bigint NOT NULL AUTO_INCREMENT,
  servicename varchar(100),
  instanceid varchar(100),
  category varchar(100),
  type int(2) NOT NULL DEFAULT 0 COMMENT '1-globaltx, 2-compensation, 3-autocompensation, 4-bizinfotokafka, 5-txmonitor, 6-alert, 7-schedule, 8-globaltxfaulttolerant, 9-compensationfaulttolerant, 10-autocompensationfaulttolerant, 11-pauseglobaltx, 50-accidentreport, 51-sqlmonitor  if values are less than 50, then configs for server, otherwise configs for client.',
  status int(1) NOT NULL DEFAULT 0 COMMENT '0-normal, 1-historical, 2-dumped',
  ability int(1) NOT NULL DEFAULT 1 COMMENT '0-do not provide ability, 1-provide ability     ps: the client''s ability inherits the global ability.',
  value varchar(100) NOT NULL,
  remark varchar(500),
  updatetime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id) USING BTREE,
  UNIQUE INDEX pk_id(id) USING BTREE,
	INDEX index_type(type) USING BTREE
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS Accident (
  id bigint NOT NULL AUTO_INCREMENT,
  servicename varchar(100),
  instanceid varchar(100),
  globaltxid varchar(36) NOT NULL,
  localtxid varchar(36) NOT NULL,
  type int(1) NOT NULL DEFAULT 0 COMMENT '1-rollback_error, 2-send_message_error',
  status int(1) NOT NULL DEFAULT 0 COMMENT '0-sending, 1-send_ok, 2-send_fail, 3-successful, 4-failed',
  bizinfo varchar(1000),
  remark varchar(500),
  createtime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completetime datetime,
  PRIMARY KEY (id)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS TableField (
  id bigint NOT NULL AUTO_INCREMENT,
  tablename varchar(50) NOT NULL,
  field varchar(50) NOT NULL,
  fieldname varchar(50) NOT NULL,
  type varchar(20) NOT NULL,
  fieldlength int(8) NOT NULL,
  decimalpoint int(1) NOT NULL DEFAULT 0,
  notnull varchar(5) NOT NULL DEFAULT 'false' COMMENT 'true/false',
  display varchar(5) NOT NULL DEFAULT 'false' COMMENT 'true/false',
  showorder int(4) NOT NULL,
  comment varchar(500),
  createtime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS DataDictionary (
  id bigint NOT NULL AUTO_INCREMENT,
  name varchar(50) NOT NULL,
  code varchar(50) NOT NULL,
  remark varchar(500),
  createtime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS DataDictionaryItem (
  id bigint NOT NULL AUTO_INCREMENT,
  ddcode varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  code varchar(50) NOT NULL,
  value varchar(50) NOT NULL,
  showorder int(2) NOT NULL DEFAULT 1,
  visible int(1) NOT NULL DEFAULT 1,
  remark varchar(500),
  createtime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET=utf8mb4 $$

-- Function TXLE_DECODE is used for supporting some status-codes/type-codes when search rows data.
DROP FUNCTION IF EXISTS TXLE_DECODE;$$
CREATE FUNCTION TXLE_DECODE(P_KEY VARCHAR(50), P VARCHAR(10))
RETURNS VARCHAR(50)
BEGIN
  DECLARE v_name VARCHAR(50);
  SELECT name INTO v_name FROM DataDictionaryItem WHERE value = P AND ddcode = P_KEY;
  RETURN v_name;
END $$

CREATE TABLE IF NOT EXISTS BusinessDBLatestDetail (
  id bigint(0) NOT NULL AUTO_INCREMENT,
  timestamp bigint(0) NOT NULL COMMENT '时间戳类似版本号，写入数据时需大于记录中最大的时间戳',
  node varchar(50) NULL COMMENT '数据库实例节点标识',
  dbschema varchar(20) NULL COMMENT '数据库名称',
  tablename varchar(30) NULL COMMENT '数据表名称',
  field varchar(30) NULL COMMENT '字段名称',
  fieldtype varchar(20) NULL,
  isprimarykey int(1) NOT NULL DEFAULT 0 COMMENT '是否为主键，1-主键，0-非主键',
  createtime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET=utf8mb4 $$

CREATE TABLE IF NOT EXISTS BusinessDBBackupInfo (
  id bigint(0) NOT NULL AUTO_INCREMENT,
  servicename varchar(50) NOT NULL,
  instanceid varchar(50) NOT NULL,
  dbnodeid varchar(50) NOT NULL COMMENT '数据库实例节点标识',
  dbschema varchar(50) NOT NULL COMMENT '数据库实例名称',
  backuptablename varchar(50) NOT NULL COMMENT '备份表名称',
  status int(1) NOT NULL DEFAULT 1 COMMENT '1-正常，2-异常',
  createtime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET=utf8mb4 $$