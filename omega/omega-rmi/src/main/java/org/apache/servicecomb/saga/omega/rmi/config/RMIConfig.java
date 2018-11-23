package org.apache.servicecomb.saga.omega.rmi.config;

import org.apache.servicecomb.saga.omega.rmi.accidentplatform.AccidentPlatformService;
import org.apache.servicecomb.saga.omega.rmi.accidentplatform.IAccidentPlatformService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * This config is imported to the EnableOmega annotation.
 * 
 * @author Gannalyo
 * @since 2018-07-30
 */
@Configuration
@EnableAspectJAutoProxy
public class RMIConfig {

	@Value("${utx.accident.platform.address:\"\"}")
	private String accidentPlatformAddress;

	@Bean
	IAccidentPlatformService accidentPlatformService() {
		return new AccidentPlatformService(accidentPlatformAddress);
	}

}
