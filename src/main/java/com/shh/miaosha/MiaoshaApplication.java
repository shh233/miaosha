package com.shh.miaosha;

import com.shh.miaosha.dao.UserDOMapper;
import com.shh.miaosha.dao.UserPasswordDOMapper;
import com.shh.miaosha.dataobject.UserDO;
import com.shh.miaosha.dataobject.UserPasswordDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication(scanBasePackages = {"com.shh.miaosha"})
@MapperScan("com.shh.miaosha.dao")
public class MiaoshaApplication {


    public static void main(String[] args) {
        SpringApplication.run(MiaoshaApplication.class, args);
    }

}
