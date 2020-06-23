package com.shh.miaosha.service;

import com.shh.miaosha.service.model.OrderModel;

public interface OrderService {

    //创建订单
    public OrderModel createOrder(Integer userId, Integer itemId, Integer amount, String stockLogId) throws Exception;
}
