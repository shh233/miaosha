package com.shh.miaosha.service.Impl;

import com.shh.miaosha.dao.*;
import com.shh.miaosha.dataobject.*;
import com.shh.miaosha.error.EmBusinessError;
import com.shh.miaosha.error.ErrorException;
import com.shh.miaosha.service.ItemService;
import com.shh.miaosha.service.OrderService;
import com.shh.miaosha.service.PromoService;
import com.shh.miaosha.service.UserService;
import com.shh.miaosha.service.model.ItemModel;
import com.shh.miaosha.service.model.OrderModel;
import com.shh.miaosha.service.model.PromoModel;
import com.shh.miaosha.service.model.UserModel;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImp implements OrderService {

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderModel createOrder(Integer userId, Integer itemId, Integer amount, String stockLogId) throws Exception{

        //校验下单状态：下单的商品是否存在，用户是否存在，下单的数量是都小于等于商品的库存
        //UserModel userModel = userService.getUserById(userId);
        UserModel userModel = userService.getUserModelInCache(userId);
        if(userModel == null){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "用户不存在");

        }

        //ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemModelInCache(itemId);
        if(itemModel == null){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "商品不存在");

        }

        if(amount<=0 || amount>99){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, "购买数量不正确（1-99）");

        }

        //落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result){
            throw new ErrorException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        itemService.increaseSales(itemId, amount);

        //订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setAmount(amount);
        orderModel.setItemId(itemId);
        orderModel.setUserId(userId);

        PromoModel promoModel = promoService.getPromoModelByItemId(itemId);
        //秒杀价
        if(promoModel!=null && promoModel.getStatus()!=3){

            orderModel.setItemPrice(promoModel.getPromoItemPrice());
            orderModel.setOrderPrice(promoModel.getPromoItemPrice().multiply(new BigDecimal(amount)));
        }
        else{
            orderModel.setItemPrice(itemModel.getPrice());
            orderModel.setOrderPrice(itemModel.getPrice().multiply(new BigDecimal(amount)));

        }

        //生成交易流水号
        orderModel.setId(createOrderNo());

        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());

        orderDOMapper.insertSelective(orderDO);

        //设置库存流水的状态
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);

        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        //返回前端
        return orderModel;
    }

    public String createOrderNo(){
        //总共16位
        // 8位时间
        StringBuilder str = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        String dataTime = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        str.append(dataTime);

        // 6位自增序列
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        int val = sequenceDO.getCurrentValue();
        String current = String.valueOf(val);
        sequenceDO.setCurrentValue(val + sequenceDO.getStep());
        for(int i=0; i<6-current.length(); i++){
            str.append("0");
        }
        str.append(current);
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);

        // 2位分库分表位
        str.append("00");

        return str.toString();
    }
}
