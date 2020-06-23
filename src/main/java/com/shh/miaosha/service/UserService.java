package com.shh.miaosha.service;

import com.shh.miaosha.error.ErrorException;
import com.shh.miaosha.service.model.UserModel;
import org.apache.ibatis.annotations.Param;

public interface UserService {
    UserModel getUserById(Integer id); //根据Id查找用户
    UserModel getUserModelInCache(Integer id) throws Exception;//在redis中查找用户
    void register(UserModel userModel) throws Exception; //注册
    UserModel login(String telphone, String password) throws Exception; //登录

}




