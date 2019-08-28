/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

public final class RecoveryPolicyFactory {
  private static final RecoveryPolicy DEFAULT_RECOVERY = new DefaultRecovery();

  private static final RecoveryPolicy FORWARD_RECOVERY = new ForwardRecovery();

  private RecoveryPolicyFactory() {
  }

  /**
   * If retries == 0, use the default recovery to execute only once.
   * If retries > 0, it will use the forward recovery and retry the given times at most.
   * If retries < 0, it will use the forward recovery and retry forever until interrupted.
   * @param retries times for retrying.
   * @return RecoveryPolicy
   */
  static RecoveryPolicy getRecoveryPolicy(int retries) {
    // To fix the below expression, to return FORWARD_RECOVERY instance when the retries' value is more then zero only. Avoiding the negative number. By Gannalyo
    return retries != 0 ? FORWARD_RECOVERY : DEFAULT_RECOVERY;
  }
}
