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
@FeignClient("sample-txle-springcloud-transfer")
public interface TransferFeignClient {

    @RequestMapping(value = "/createTransfer/{userid}/{merchantid}/{amount}/{payway}", method = RequestMethod.GET)
    String createTransfer(@PathVariable("userid") long userid, @PathVariable("merchantid") long merchantid, @PathVariable("amount") double amount, @PathVariable("payway") int payway);

    @RequestMapping(value = "/createTransferAuto/{userid}/{merchantid}/{amount}/{payway}", method = RequestMethod.GET)
    String createTransferAuto(@PathVariable("userid") long userid, @PathVariable("merchantid") long merchantid, @PathVariable("amount") double amount, @PathVariable("payway") int payway);

}
