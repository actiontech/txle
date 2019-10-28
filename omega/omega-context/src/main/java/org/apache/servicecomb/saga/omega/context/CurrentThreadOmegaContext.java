/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

/**
 * A cache tool for compensation.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
public final class CurrentThreadOmegaContext {

	private static final ThreadLocal<OmegaContextServiceConfig> CUR_THREAD_OMEGA_CONTEXT = new ThreadLocal<>();

	private CurrentThreadOmegaContext() {
	}

	public static void putThreadGlobalLocalTxId(OmegaContextServiceConfig context) {
		CUR_THREAD_OMEGA_CONTEXT.set(context);
	}

	public static OmegaContextServiceConfig getContextFromCurThread() {
		return CUR_THREAD_OMEGA_CONTEXT.get();
	}
	public static String getGlobalTxIdFromCurThread() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.globalTxId();
		}
		return "";
	}

	public static String getLocalTxIdFromCurThread() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.localTxId();
		}
		return "";
	}

	public static String getServiceNameFromCurThread() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.serviceName();
		}
		return "";
	}

	public static boolean isAutoCompensate() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.isAutoCompensate();
		}
		return false;
	}

	public static boolean isEnabledAutoCompensateTx() {
		OmegaContextServiceConfig context = CUR_THREAD_OMEGA_CONTEXT.get();
		if (context != null) {
			return context.isEnabledAutoCompensateTx();
		}
		return false;
	}

	public static void clearCache() {
		CUR_THREAD_OMEGA_CONTEXT.remove();
	}

}
