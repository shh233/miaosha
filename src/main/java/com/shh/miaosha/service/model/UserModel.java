package com.shh.miaosha.service.model;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class UserModel implements Serializable {
    private Integer id;

    @NotBlank(message = "姓名不能不填")
    private String name;

    @NotNull(message = "性别不能为空")
    private Byte gender;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于0岁")
    @Max(value = 150, message = "年龄不能大于150岁")
    private Integer age;

    @NotNull(message = "手机号不能为空")
    private String telphone;
    
    private String registerMode;
    private String thirdPartyId;

    @NotNull(message = "密码不能为空")
    private String encrptPassword;
}
