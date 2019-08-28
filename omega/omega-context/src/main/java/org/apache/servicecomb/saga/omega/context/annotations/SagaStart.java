/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the annotated method will start a saga.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface SagaStart {

  /**
   * Saga timeout, in seconds. <br>
   * Default value is 0, which means never timeout. It means also never timeout if value is negative number. By Gannalyo.
   *
   * @return
   */
  int timeout() default 0;

  String category() default "";
}
