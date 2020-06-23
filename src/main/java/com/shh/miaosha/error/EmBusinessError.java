package com.shh.miaosha.error;

public enum EmBusinessError implements CommonError {

    //通用错误类型
    PARAMITER_VALIDATION_ERROR(10001, "参数不合法"),

    //未知错误
    UNKNOW_ERROE(10002, "未知错误"),

    //用户不存在
    USER_NOT_EXITS(20001, "用户不存在"),
    USER_NOT_LOGIN(20002, "用户还未登录"),

    //30000开头为交易错误
    STOCK_NOT_ENOUGH(30001, "库存不足"),
    RATELIMITER(30002, "活动太火爆，请稍后再试");



    private int errorCode;
    private String errorMSG;

    EmBusinessError(int errorCode, String errorMSG) {
        this.errorCode = errorCode;
        this.errorMSG = errorMSG;
    }

    @Override
    public int getErrorCode() {
        return this.errorCode;
    }

    @Override
    public String getErrorMsg() {
        return this.errorMSG;
    }

    @Override
    public CommonError setErrorMsg(String msg) {
        this.errorMSG = msg;
        return this;
    }
}
