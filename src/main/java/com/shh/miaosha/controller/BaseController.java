package com.shh.miaosha.controller;

import com.shh.miaosha.Response.CommonReturnType;
import com.shh.miaosha.error.EmBusinessError;
import com.shh.miaosha.error.ErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;

public class BaseController {

    public static final String CONTENT_TYPE_FORMED = "application/x-www-form-urlencoded";

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonReturnType handleException(@RequestParam(name = "ex") Exception ex){
        HashMap<String, Object> map = new HashMap<>();
        if(ex instanceof ErrorException){
            ErrorException errorException = (ErrorException)ex;
            map.put("errCode", errorException.getErrorCode());
            map.put("errMsg", errorException.getErrorMsg());
        }
        else{
            map.put("errCode", ""+ EmBusinessError.UNKNOW_ERROE.getErrorCode());
            map.put("errMsg", EmBusinessError.UNKNOW_ERROE.getErrorMsg());
        }

        return CommonReturnType.create(map, "fail");
    }

}
