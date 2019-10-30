/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@SpringBootApplication
@EnableEurekaServer
public class Eureka222Application {
    public static void main(String[] args) {
        SpringApplication.run(Eureka222Application.class, args);
    }
}
