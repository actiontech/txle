/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.TxleConstants;

public class OmegaException extends RuntimeException {
  public OmegaException(String message) {
    super(message);
  }

  public OmegaException(String cause, Throwable throwable) {
    super(TxleConstants.logErrorPrefixWithTime() + cause, throwable);
  }
}
