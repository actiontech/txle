/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.cache;

import net.sf.ehcache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Gannalyo
 * @Cacheable 方法执行前先看缓存中是否有数据，如果有直接返回。如果没有就调用方法，并将方法返回值放入缓存
 * @CachePut 无论怎样都会执行方法，并将方法返回值放入缓存
 * @CacheEvict 将数据从缓存中删除
 * @Caching 可通过此注解组合多个注解策略在一个方法上面
 */
@Configuration
public class EhCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return CacheManager.create(this.getClass().getResourceAsStream("/ehcache.xml"));
    }

    @Bean
    public EhCacheCacheManager cacheManager(CacheManager cacheManager) {
        return new EhCacheCacheManager(cacheManager);
    }

    @Bean
    public EhCacheManagerFactoryBean ehcache() {
        EhCacheManagerFactoryBean ehCacheManagerFactoryBean = new EhCacheManagerFactoryBean();
        ehCacheManagerFactoryBean.setConfigLocation(new ClassPathResource("ehcache.xml"));
        return ehCacheManagerFactoryBean;
    }

}
