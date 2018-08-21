package org.apache.servicecomb.saga.omega.transaction.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCompensable {

	int retries() default 0;

	int retryDelayInMilliseconds() default 0;

	/**
	 * unit seconds
	 * @return
	 */
	int timeout() default 0;
}
