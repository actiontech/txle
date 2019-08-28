/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("annotation-checking")
@Component
class MisconfiguredService {

  @Compensable(compensationMethod = "none")
  void doSomething() {
  }
}
