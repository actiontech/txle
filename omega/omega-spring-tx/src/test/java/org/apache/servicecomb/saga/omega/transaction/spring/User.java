/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String email;

  User() {
  }

  User(String username, String email) {
    this.username = username;
    this.email = email;
  }

  long id() {
    return id;
  }

  String username() {
    return username;
  }

  String email() {
    return email;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return id.equals(user.id) &&
        Objects.equals(username, user.username) &&
        Objects.equals(email, user.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, email);
  }

  @Override
  public String toString() {
    return "User{" +
        "username='" + username + '\'' +
        ", email='" + email + '\'' +
        '}';
  }
}
