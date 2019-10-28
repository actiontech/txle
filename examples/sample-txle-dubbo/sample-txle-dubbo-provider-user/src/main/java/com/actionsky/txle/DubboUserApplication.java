/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.servicecomb.saga.omega.spring.EnableOmega;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@SpringBootApplication
@EnableDubbo
@EnableOmega
public class DubboUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(DubboUserApplication.class, args);
    }
}
