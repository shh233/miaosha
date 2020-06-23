package com.shh.miaosha.service;

import com.shh.miaosha.service.model.PromoModel;

public interface PromoService {

    //根据itemId获取---即将开始秒杀和正在开始秒杀---的PromoModel
    public PromoModel getPromoModelByItemId(Integer itemId) throws Exception;

    //发布活动, 将promoModel存入到缓存中
    public void publishPromo(Integer promoId) throws Exception;

    //管理令牌秒杀生成
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) throws Exception;
}
