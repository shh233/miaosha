package com.shh.miaosha.service;

public interface CacheService {

    //存方法
    public void setCommonCache(String key, Object val);

    //取方法
    public Object getCommonCache(String key);
}
