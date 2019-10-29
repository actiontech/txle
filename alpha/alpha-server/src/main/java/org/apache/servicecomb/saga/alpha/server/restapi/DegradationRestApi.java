/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.restapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DegradationRestApi {
	private static final Logger LOG = LoggerFactory.getLogger(DegradationRestApi.class);

	@GetMapping("/disableGlobalTransaction")
	public String disableGlobalTransaction() {
		return "";
	}

	@GetMapping("/disableCompensation")
	public String disableCompensation() {
		return "";
	}

	@GetMapping("/disableAutoCompensation")
	public String disableAutoCompensation() {
		return "";
	}

	@GetMapping("/disableLogReport")
	public String disableLogReport() {
 //		this.disableKafkaLogReport();
		return "";
	}

	@GetMapping("/disableKafkaLogReport")
	public String disableKafkaLogReport() {
		return "";
	}

	@GetMapping("/disableAccidentReport")
	public String disableAccidentReport() {
		return "";
	}

	@GetMapping("/disableTransactionMonitoring")
	public String disableTransactionMonitoring() {
		return "";
	}

	@GetMapping("/disableSqlMonitoring")
	public String disableSqlMonitoring() {
		return "";
	}

	@GetMapping("/disableAltering")
	public String disableAltering() {
		return "";
	}

}
