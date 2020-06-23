package com.shh.miaosha.controller;

import com.alibaba.druid.util.StringUtils;
import com.shh.miaosha.Response.CommonReturnType;
import com.shh.miaosha.controller.viewobject.UserVO;
import com.shh.miaosha.error.CommonError;
import com.shh.miaosha.error.EmBusinessError;
import com.shh.miaosha.error.ErrorException;
import com.shh.miaosha.service.UserService;
import com.shh.miaosha.service.model.UserModel;
import org.apache.ibatis.annotations.Param;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true" , allowedHeaders = "*")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    RedisTemplate redisTemplate;

    //生成otp验证码
    @RequestMapping(value = "/getotp" , method = {RequestMethod.POST} , consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType getOtp(@RequestParam(name = "telphone") String telphone) throws Exception {
        if(telphone==null || StringUtils.isEmpty(telphone)){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "手机号不能空");
        }
        //1.随机生成otp
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otp = String.valueOf(randomInt);

        /*//2.将otp和手机号绑定起来，最好使用redis ,这里我们暂时使用httpsession
            //服务器会为每一个用户 创建一个独立的HttpSession，并且有默认的存活时间
        httpServletRequest.getSession().setAttribute(telphone, otp);*/

        //将电话号码和otp存入redis中
        redisTemplate.opsForValue().set(telphone, otp);
        redisTemplate.expire(telphone, 1, TimeUnit.MINUTES); //过期时间设为1分钟


        //3.将otp验证码发送给用户
        System.out.println("telphone:"+telphone+" & otp:"+redisTemplate.opsForValue().get(telphone));

        return CommonReturnType.create(null);
    }


    //用户注册
    @RequestMapping(value = "/register" , method = {RequestMethod.POST} , consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType register(@RequestParam(name = "telphone") String telphone,
                                     @RequestParam(name = "password") String password,
                                     @RequestParam(name = "age") Integer age,
                                     @RequestParam(name = "gender") Byte gender,
                                     @RequestParam(name = "otpCode") String otpCode,
                                     @RequestParam(name = "name") String name
                                     ) throws Exception{  //和HTML中ajax的参数列表一致，名字一样

        /*//验证电话对应otp和用户输入的otp是否一样, 一定要是this,this,this
        String getOtp = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        */

        //将redis中存储的otp获得
        String getOtp =(String)redisTemplate.opsForValue().get(telphone);
        if(!StringUtils.equals(otpCode, getOtp)){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "短信验证码不正确");
        }



        //用户注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setAge(age);
        userModel.setGender(gender);
        userModel.setTelphone(telphone);
        userModel.setEncrptPassword(EncodeByMD5(password));
        userService.register(userModel);
        return CommonReturnType.create(null);

    }
    //自定义MD5加密计算方式
    public String EncodeByMD5(String str) throws Exception {
        //确定计算方式
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();

        //加密字符串
        String newStr= base64en.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }


    //用户登陆
    @RequestMapping(value = "/login" , method = {RequestMethod.POST} , consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType login(@RequestParam(name = "telphone") String telphone,
                                  @RequestParam(name = "password") String password) throws Exception {
        //空判断
        if(StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "用户名或者密码为空");
        }

        //参数检验：1.判断用户是否存在； 2.判断手机号和密码是否对应
        password = EncodeByMD5(password);
        UserModel userModel = userService.login(telphone, password);

       /* //将登陆凭证加入到用户登录成功的session内
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);
*/
        //若用户登录成功后将对应的信息存入redis中
        //1.生成UUID作为用户登录凭证
        String tokenUUID = UUID.randomUUID().toString().replace("-", "");

        //2.存入redis中
        redisTemplate.opsForValue().set(tokenUUID, userModel);

        //3.设置过期时间, 一小时
        redisTemplate.expire(tokenUUID,1, TimeUnit.HOURS);

        //4.将登录凭证返回给前端
        return CommonReturnType.create(tokenUUID);
    }

    @RequestMapping("/get")
    public CommonReturnType getUser(@Param("id") int id) throws Exception {
        UserModel userModel = userService.getUserById(id);
        if(userModel == null){
            //throw new ErrorException(EmBusinessError.USER_NOT_EXITS);
            userModel.setAge(1);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return CommonReturnType.create(userVO);
    }


}
