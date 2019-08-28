/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import javax.persistence.*;

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;

@Entity
@Table(name = "Command")
public class Command {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long surrogateId;

  private long eventId;
  private String serviceName;
  private String instanceId;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String compensationMethod;
  private byte[] payloads;
  private String status;

  private String category;

  @Version
  private long version;

  Command() {
  }

  private Command(long id,
      String serviceName,
      String instanceId,
      String globalTxId,
      String localTxId,
      String parentTxId,
      String compensationMethod,
      String category,
      byte[] payloads,
      String status) {

    this.eventId = id;
    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.compensationMethod = compensationMethod;
    this.category = category;
    this.payloads = payloads;
    this.status = status;
  }

  public Command(long id,
      String serviceName,
      String instanceId,
      String globalTxId,
      String localTxId,
      String parentTxId,
      String compensationMethod,
      String category,
      byte[] payloads) {

    this(id, serviceName, instanceId, globalTxId, localTxId, parentTxId, compensationMethod, category, payloads, NEW.name());
  }

  public Command(TxEvent event) {
    this(event.id(),
        event.serviceName(),
        event.instanceId(),
        event.globalTxId(),
        event.localTxId(),
        event.parentTxId(),
        event.compensationMethod(),
        event.category(),
        event.payloads());
  }

  String serviceName() {
    return serviceName;
  }

  String instanceId() {
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

  public String compensationMethod() {
    return compensationMethod;
  }

  byte[] payloads() {
    return payloads;
  }

  String status() {
    return status;
  }

  long id() {
    return surrogateId;
  }

  String category() {
    return category;
  }

  public long getEventId() {
    return eventId;
  }

  @Override
  public String toString() {
    return "Command{"
            + "eventId=" + eventId
            + ", serviceName='" + serviceName + '\''
            + ", instanceId='" + instanceId + '\''
            + ", globalTxId='" + globalTxId + '\''
            + ", localTxId='" + localTxId + '\''
            + ", parentTxId='" + parentTxId + '\''
            + ", compensationMethod='" + compensationMethod + '\''
            + ", category='" + category + '\''
            + '}';
  }
}
