/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import org.apache.servicecomb.saga.omega.transaction.spring.annotations.OmegaContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("omega-context-aware-checking")
class MisconfiguredAnnotation {
  @OmegaContextAware
  private final User user = new User();
}
