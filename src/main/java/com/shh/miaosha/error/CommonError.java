package com.shh.miaosha.error;

public interface CommonError {
    public int getErrorCode();
    public String getErrorMsg();
    public CommonError setErrorMsg(String msg);
}
