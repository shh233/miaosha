package com.shh.miaosha.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

@Component
public class ValidationImp implements InitializingBean {

    private Validator validator;

    //通过validator工厂实例化
    @Override
    public void afterPropertiesSet() throws Exception {
        this.validator = (Validator) Validation.buildDefaultValidatorFactory().getValidator();
    }

    //实现校验方法并返回校验结果
    public ValidationResult validatate(Object bean){
        ValidationResult validationResult = new ValidationResult();
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);

        if(constraintViolationSet.size()>0){  //有错误
            validationResult.setHasError(true);
            constraintViolationSet.forEach(constraintViolation -> {
                String errMsg = constraintViolation.getMessage();
                String propertyName = constraintViolation.getPropertyPath().toString();
                validationResult.getErrorMsgMap().put(propertyName, errMsg);
            });
        }

        return validationResult;
    }
}
