package com.shh.miaosha.service;

import com.shh.miaosha.service.model.ItemModel;

import java.util.List;

public interface ItemService {

    //1.创建商品
    public ItemModel createItem(ItemModel itemModel) throws Exception;


    //2.商品列表浏览
    public List<ItemModel> listItem();

    //3.商品详情浏览
    public ItemModel getItemById(Integer id) throws Exception;

    //item及promoModel缓存模型
    public ItemModel getItemModelInCache(Integer id) throws Exception;

    //4.减库存
    public boolean decreaseStock(Integer itemId, Integer amount) throws Exception;

    //5.增加销量
    public boolean increaseSales(Integer itemId, Integer amount) throws Exception;

    //初始化订单流水
    public String initStockLog(Integer itemId, Integer amount);
}
