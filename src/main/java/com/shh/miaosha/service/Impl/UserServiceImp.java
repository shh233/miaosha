package com.shh.miaosha.service.Impl;

import com.mysql.cj.util.StringUtils;
import com.shh.miaosha.dao.UserDOMapper;
import com.shh.miaosha.dao.UserPasswordDOMapper;
import com.shh.miaosha.dataobject.UserDO;
import com.shh.miaosha.dataobject.UserPasswordDO;
import com.shh.miaosha.error.EmBusinessError;
import com.shh.miaosha.error.ErrorException;
import com.shh.miaosha.service.UserService;
import com.shh.miaosha.service.model.UserModel;
import com.shh.miaosha.validator.ValidationImp;
import com.shh.miaosha.validator.ValidationResult;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImp implements UserService {
    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidationImp validationImp;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public UserModel getUserById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if(userDO == null) return null;
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(id);
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO,userModel);
        if(userPasswordDO != null){
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }
        return userModel;
    }

    @Override
    public UserModel getUserModelInCache(Integer id) throws Exception{
        UserModel userModel = new UserModel();
        userModel = (UserModel) redisTemplate.opsForValue().get("user_validate"+id);
        if(userModel == null){
            userModel = this.getUserById(id);
            redisTemplate.opsForValue().set("user_validate"+id, userModel);
            redisTemplate.expire("user_validate"+id, 1, TimeUnit.MINUTES);
        }
        return userModel;
    }

    @Override
    //@Transactional
    public void register(UserModel userModel) throws Exception {
        if(userModel == null){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR);
        }

        //参数校验
        ValidationResult result = validationImp.validatate(userModel);
        if(result.isHasError()){ //有错误
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, result.getErrorMsg());
        }

        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        try {
            userDOMapper.insertSelective(userDO);
        }catch (Exception e){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "手机号已经注册");
        }

        UserPasswordDO userPasswordDO = new UserPasswordDO();
        int id = userDO.getId();
        userPasswordDO.setUserId(userDO.getId());
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        //这里的密码已经在controller层经过MD5加密了

        //在一个事务插入两条记录
        userPasswordDOMapper.insertSelective(userPasswordDO);

    }

    @Override
    public UserModel login(String telphone, String password) throws ErrorException {
        //空判断
        if(StringUtils.isNullOrEmpty(telphone) || StringUtils.isNullOrEmpty(password)){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "用户名或者密码为空");
        }

        //通过手机号获取用户,判断用户是否存在
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if(userDO == null){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "用户不存在");
        }

        //判断用户名和密码是否匹配
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        if(!org.apache.commons.lang3.StringUtils.equals(userPasswordDO.getEncrptPassword(), password)){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "用户名或者密码错误");
        }

        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);
        userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        return userModel;

    }
}
