/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.restapi;

import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CacheRestApi {
    @Autowired
    private ITxleCache txleCache;

    @PostMapping("/putConfigCache")
    public void putConfigCache(@RequestBody String cache) {
        if (cache != null) {
            String[] arrKV = cache.split(",");
            txleCache.putLocalConfigCache(arrKV[0], Boolean.valueOf(arrKV[1]));
        }
    }

    @PostMapping("/removeConfigCache")
    public void removeConfigCache(@RequestBody String cache) {
        if (cache != null) {
            txleCache.removeLocalConfigCache(cache.split(",")[0]);
        }
    }

    @PostMapping("/putTxSuspendStatusCache")
    public void putTxSuspendStatusCache(@RequestBody String cache) {
        if (cache != null) {
            String[] arrKV = cache.split(",");
            txleCache.putLocalTxSuspendStatusCache(arrKV[0], Boolean.valueOf(arrKV[1]), Integer.parseInt(arrKV[2]));
        }
    }

    @PostMapping("/removeTxSuspendStatusCache")
    public void removeTxSuspendStatusCache(@RequestBody String cache) {
        if (cache != null) {
            txleCache.removeLocalTxSuspendStatusCache(cache.split(",")[0]);
        }
    }

    @PostMapping("/putTxAbortStatusCache")
    public void putTxAbortStatusCache(@RequestBody String cache) {
        if (cache != null) {
            String[] arrKV = cache.split(",");
            txleCache.putLocalTxAbortStatusCache(arrKV[0], Boolean.valueOf(arrKV[1]), Integer.parseInt(arrKV[2]));
        }
    }

    @PostMapping("/removeTxAbortStatusCache")
    public void removeTxAbortStatusCache(@RequestBody String cache) {
        if (cache != null) {
            txleCache.removeLocalTxAbortStatusCache(cache.split(",")[0]);
        }
    }

}
