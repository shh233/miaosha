package com.shh.miaosha.validator;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

@Data
public class ValidationResult {

    private boolean hasError = false;  //验证结果是否有错误
    private HashMap<String, String> errorMsgMap = new HashMap<>();  //错误的结果集

    //获取结果集
    public String getErrorMsg(){
        return StringUtils.join(errorMsgMap.values().toArray(), ",");
    }

}
