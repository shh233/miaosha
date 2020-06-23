package com.shh.miaosha.Response;

import lombok.Data;

@Data
public class CommonReturnType {
    private String status;
    private Object data;

    public static CommonReturnType create(Object object){
        return create(object, "success");
    }

    public static CommonReturnType create(Object object, String status){
        CommonReturnType commonReturnType = new CommonReturnType();
        commonReturnType.setData(object);
        commonReturnType.setStatus(status);
        return commonReturnType;
    }

}
