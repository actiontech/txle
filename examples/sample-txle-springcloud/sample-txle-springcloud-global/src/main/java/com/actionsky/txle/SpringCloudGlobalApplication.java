/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle;

import org.apache.servicecomb.saga.omega.spring.EnableOmega;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@SpringBootApplication
@EnableDiscoveryClient //@EnableEurekaClient 后者包含前者，但只适用eureka注册中心场景
@EnableOmega
//@EnableFeignClients
@EnableFeignClients(basePackages = "com.actionsky.txle.api")
public class SpringCloudGlobalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringCloudGlobalApplication.class, args);
    }
}
