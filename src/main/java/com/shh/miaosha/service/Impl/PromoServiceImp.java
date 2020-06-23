package com.shh.miaosha.service.Impl;

import com.shh.miaosha.dao.ItemDOMapper;
import com.shh.miaosha.dao.PromoDOMapper;
import com.shh.miaosha.dataobject.PromoDO;
import com.shh.miaosha.error.EmBusinessError;
import com.shh.miaosha.error.ErrorException;
import com.shh.miaosha.service.ItemService;
import com.shh.miaosha.service.PromoService;
import com.shh.miaosha.service.model.ItemModel;
import com.shh.miaosha.service.model.PromoModel;
import org.checkerframework.checker.units.qual.A;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImp implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PromoModel getPromoModelByItemId(Integer itemId) throws Exception {
        //1，通过itemID获取promoDO
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        //System.out.println("promoDO"+promoDO);
        if(promoDO == null){
            return null;
        }

        //2.将promoDO转化成PromoModel
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));

        //判断秒杀的状态
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1); //表示秒杀即将开始

        }
        else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);//表示秒杀活动已经结束
            return null;
        }
        else{
            promoModel.setStatus(2);
        }
        return promoModel;

    }

    @Override
    public void publishPromo(Integer promoId) throws Exception {

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);

        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());

        //将库存同步到redis中
        redisTemplate.opsForValue().set("promo_item_stock"+itemModel.getId(), itemModel.getStock());

        //秒杀大闸，将最多的秒杀令牌数量存入redis中
        redisTemplate.opsForValue().set("promo_door_count_"+itemModel.getId(),itemModel.getStock()*5);
    }



    //生成秒杀令牌
    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) throws Exception {


        //判断秒杀令牌是否用完
        Long result = redisTemplate.opsForValue().increment(
                "promo_door_count_"+itemId,-1);
        if(result < 0){
            return null;
        }

        //判断是否售罄
        if(redisTemplate.hasKey("stock_sold_out"+itemId)){
            return null;
        }

        if(promoId == null || itemId == null || userId == null){
            return null;
        }


        String promoToken = UUID.randomUUID().toString().replace("-","");

        redisTemplate.opsForValue().set("promo_"+promoId+"_item_"+itemId+"_user_"+userId, promoToken);
        redisTemplate.expire("promo_"+promoId+"_item_"+itemId+"_user_"+userId, 5, TimeUnit.MINUTES);

        return promoToken;
    }
}
