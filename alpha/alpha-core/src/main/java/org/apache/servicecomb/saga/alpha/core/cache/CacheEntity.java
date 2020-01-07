/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class CacheEntity implements Serializable {
    private String key;
    private Object value;
    private long expire;

    public CacheEntity(String key, Object value) {
        this.key = key;
        this.value = value;
        // default expire is 30 minutes.
        this.expire = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30);
    }

    public CacheEntity(String key, Object value, long expire) {
        this.key = key;
        this.value = value;
        this.expire = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(expire > 0 ? expire : 30);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public boolean expired() {
        return expire < System.currentTimeMillis();
    }

}