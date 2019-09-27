/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the annotated method will start a sub-transaction. <br>
 * A <code>@Compensable</code> method should satisfy below requirements:
 * <ol>
 *   <li>all parameters are serialized</li>
 *   <li>is idempotent</li>
 *   <li>the object instance which @Compensable method resides in should be stateless</li>
 *   <li>if compensationMethod exists, both methods must be commutative, see this
 *   <a href="https://servicecomb.incubator.apache.org/docs/distributed_saga_2/">link</a>.</li>
 * </ol>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Compensable {

  /**
   * 0 or less -1, never retry
   * -1, retry forever
   * more than 0, retry for retries number.
   * @return retries
   * @author Gannalyo
   */
  int retries() default 0;

  /**
   * Compensation method name.<br>
   * A compensation method should satisfy below requirements:
   * <ol>
   *   <li>has same parameter list as @Compensable method's</li>
   *   <li>all parameters are serialized</li>
   *   <li>is idempotent</li>
   *   <li>be in the same class as @Compensable method is in</li>
   * </ol>
   *
   * @return compensation method
   */
  String compensationMethod() default "";

  int retryDelayInMilliseconds() default 0;

  /**
   * <code>@Compensable</code> method timeout, in seconds. <br>
   * Default value is 0, which means never timeout.
   *
   * @return timeout
   */
  int timeout() default 0;
}
