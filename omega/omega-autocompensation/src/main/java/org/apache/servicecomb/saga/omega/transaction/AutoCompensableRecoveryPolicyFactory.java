/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

public final class AutoCompensableRecoveryPolicyFactory {
  private static final AutoCompensableRecoveryPolicy DEFAULT_RECOVERY = new AutoCompensableRecovery();

  private static final AutoCompensableRecoveryPolicy FORWARD_RECOVERY = new AutoCompensableForwardRecovery();

  private AutoCompensableRecoveryPolicyFactory() {
  }

  /**
   * If the value of the variable 'retries' equals 0, use the default recovery to execute only once.
   * If the value of the variable 'retries' is more than 0, it will use the forward recovery and retry the given times at most.
   * If the value of the variable 'retries' is less than 0, it will use the forward recovery and retry forever until interrupted.
   * @param retries times for retrying
   * @return AutoCompensableRecoveryPolicy
   */
  protected static AutoCompensableRecoveryPolicy getRecoveryPolicy(int retries) {
    return retries != 0 ? FORWARD_RECOVERY : DEFAULT_RECOVERY;
  }
}
