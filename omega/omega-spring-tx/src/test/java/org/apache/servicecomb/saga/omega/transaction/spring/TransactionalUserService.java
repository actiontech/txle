/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class TransactionalUserService {
  static final String ILLEGAL_USER = "Illegal User";
  private final UserRepository userRepository;

  private int count = 0;

  @Autowired
  TransactionalUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  void resetCount() {
    this.count = 0;
  }

  @Compensable(compensationMethod = "delete")
  User add(User user) {
    if (ILLEGAL_USER.equals(user.username())) {
      throw new IllegalArgumentException("User is illegal");
    }
    return userRepository.save(user);
  }

  void delete(User user) {
    userRepository.delete(user);
  }

  @Compensable(retries = 2, compensationMethod = "delete")
  User add(User user, int count) {
    if (this.count < count) {
      this.count += 1;
      throw new IllegalStateException("Retry harder");
    }
    resetCount();
    return userRepository.save(user);
  }

  void delete(User user, int count) {
    resetCount();
    userRepository.delete(user);
  }
}
