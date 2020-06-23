package com.shh.miaosha.error;

import lombok.Data;

@Data
public class ErrorException extends Exception implements CommonError {

    private EmBusinessError emBusinessError;

    public ErrorException(EmBusinessError emBusinessError){
        super();
        this.emBusinessError = emBusinessError;
    }

    public ErrorException(EmBusinessError emBusinessError, String msg){
        super();
        this.emBusinessError = emBusinessError;
        this.emBusinessError.setErrorMsg(msg);
    }

    @Override
    public int getErrorCode() {
        return this.getEmBusinessError().getErrorCode();
    }

    @Override
    public String getErrorMsg() {
        return this.getEmBusinessError().getErrorMsg();
    }

    @Override
    public CommonError setErrorMsg(String mse) {
        return this.getEmBusinessError().setErrorMsg(mse);
    }
}
