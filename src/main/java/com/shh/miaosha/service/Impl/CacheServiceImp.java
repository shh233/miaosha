package com.shh.miaosha.service.Impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.shh.miaosha.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceImp implements CacheService {

    //Guava提供的cache类
    private Cache<String, Object> commonCache = null;

    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                .initialCapacity(10)
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES).build();
    }

    @Override
    public void setCommonCache(String key, Object val) {
        commonCache.put(key, val);
    }

    @Override
    public Object getCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
