/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "TxTimeout")
public class TxTimeout {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long surrogateId;

  private long eventId;
  private String serviceName;
  private String instanceId;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String type;
  private Date expiryTime;
  private String status;
  private String category;

  @Version
  private long version;

  TxTimeout() {
  }

  TxTimeout(long eventId, String serviceName, String instanceId, String globalTxId, String localTxId,
      String parentTxId, String type, Date expiryTime, String status, String category) {
    this.eventId = eventId;
    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.type = type;
    this.expiryTime = expiryTime;
    this.status = status;
    this.category = category;
  }

  public String serviceName() {
    return serviceName;
  }

  public String instanceId() {
    return instanceId;
  }

  public String globalTxId() {
    return globalTxId;
  }

  public String localTxId() {
    return localTxId;
  }

  public String parentTxId() {
    return parentTxId;
  }

  public String type() {
    return type;
  }

  public Date expiryTime() {
    return expiryTime;
  }

  public String status() {
    return status;
  }

  public String category() {
    return category;
  }

  @Override
  public String toString() {
    return "TxTimeout{" +
        "eventId=" + eventId +
        ", serviceName='" + serviceName + '\'' +
        ", instanceId='" + instanceId + '\'' +
        ", globalTxId='" + globalTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", type='" + type + '\'' +
        ", expiryTime=" + expiryTime +
        ", category=" + category +
        ", status=" + status +
        '}';
  }
}
