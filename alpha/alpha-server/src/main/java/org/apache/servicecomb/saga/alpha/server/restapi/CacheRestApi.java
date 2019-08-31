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
    public void putConfigCache(@RequestBody String cacheKV) {
        if (cacheKV != null) {
            String[] arrKV = cacheKV.split(",");
            txleCache.getConfigCache().put(arrKV[0], Boolean.valueOf(arrKV[1]));
        }
    }

    @PostMapping("/removeConfigCache")
    public void removeConfigCache(@RequestBody String cacheKV) {
        if (cacheKV != null) {
            txleCache.getConfigCache().remove(cacheKV.split(",")[0]);
            if (txleCache.getConfigCache().isEmpty()) {
                txleCache.getConfigCache().clear();
            }
        }
    }

    @PostMapping("/putTxStatusCache")
    public void putTxStatusCache(@RequestBody String cacheKV) {
        if (cacheKV != null) {
            String[] arrKV = cacheKV.split(",");
            txleCache.getTxSuspendStatusCache().put(arrKV[0], Boolean.valueOf(arrKV[1]));
        }
    }

    @PostMapping("/removeTxStatusCache")
    public void removeTxStatusCache(@RequestBody String cacheKV) {
        if (cacheKV != null) {
            txleCache.getTxSuspendStatusCache().remove(cacheKV.split(",")[0]);
            if (txleCache.getTxSuspendStatusCache().isEmpty()) {
                txleCache.getTxSuspendStatusCache().clear();
            }
        }
    }

}
