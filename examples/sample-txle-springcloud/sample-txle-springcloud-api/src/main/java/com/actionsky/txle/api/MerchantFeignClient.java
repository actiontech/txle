/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.api;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Gannalyo
 * @since 2019/4/3
 */
@FeignClient("sample-txle-springcloud-merchant")
public interface MerchantFeignClient {

    @RequestMapping(value = "/payMoneyToMerchant/{merchantid}/{balance}", method = RequestMethod.GET)
    String payMoneyToMerchant(@PathVariable("merchantid") long merchantid, @PathVariable("balance") double balance);

    @RequestMapping(value = "/payMoneyToMerchantAuto/{merchantid}/{balance}", method = RequestMethod.GET)
    String payMoneyToMerchantAuto(@PathVariable("merchantid") long merchantid, @PathVariable("balance") double balance);
}
