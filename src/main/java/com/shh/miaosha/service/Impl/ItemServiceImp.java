package com.shh.miaosha.service.Impl;

import com.shh.miaosha.dao.ItemDOMapper;
import com.shh.miaosha.dao.ItemStockDOMapper;
import com.shh.miaosha.dao.StockLogDOMapper;
import com.shh.miaosha.dataobject.ItemDO;
import com.shh.miaosha.dataobject.ItemStockDO;
import com.shh.miaosha.dataobject.StockLogDO;
import com.shh.miaosha.error.EmBusinessError;
import com.shh.miaosha.error.ErrorException;
import com.shh.miaosha.service.ItemService;
import com.shh.miaosha.service.PromoService;
import com.shh.miaosha.service.model.ItemModel;
import com.shh.miaosha.service.model.PromoModel;
import com.shh.miaosha.validator.ValidationImp;
import com.shh.miaosha.validator.ValidationResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ItemServiceImp implements ItemService {

    @Autowired
    private ValidationImp validationImp;

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;



    @Override
    @Transactional   //一个事务中
    public ItemModel createItem(ItemModel itemModel) throws Exception {

        //1.参数校验
        if(itemModel == null) return null;

        ValidationResult result = validationImp.validatate(itemModel);
        if(result.isHasError()){
            throw new ErrorException(EmBusinessError.PARAMITER_VALIDATION_ERROR, result.getErrorMsg());
        }

        //2.ItemModel -> dataObject
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        //System.out.println("id"+itemDO.getId());
        itemDOMapper.insertSelective(itemDO);
        //System.out.println("id"+itemDO.getId());

        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setStock(itemModel.getStock());
        itemStockDO.setItemId(itemDO.getId());
        itemStockDOMapper.insertSelective(itemStockDO);


        //3.向前端展示修改之后的ItemModel
        return getItemById(itemDO.getId());
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemModel> resList = new ArrayList<>();
        List<ItemDO> list = itemDOMapper.listItem();
        list.forEach(itemDO -> {

            ItemModel itemModel = new ItemModel();
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

            BeanUtils.copyProperties(itemDO, itemModel);
            itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
            itemModel.setStock(itemStockDO.getStock());


            resList.add(itemModel);
        });
        return resList;
    }

    @Override
    public ItemModel getItemById(Integer id) throws Exception {
        ItemModel itemModel = new ItemModel();
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(id);

        if(itemDO==null || itemStockDO==null) return null;
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());

        PromoModel promoModel = promoService.getPromoModelByItemId(itemDO.getId());
        if(promoModel != null && promoModel.getStatus()!=3){ //有正在进行 或者即将开始的秒杀活动
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    @Override
    public ItemModel getItemModelInCache(Integer id) throws Exception{

        ItemModel itemModel = new ItemModel();
        itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate"+id);
        if(itemModel == null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate"+id, itemModel);
            redisTemplate.expire("item_validate"+id, 1, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws Exception {

        //减数据库库存
        //int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);

        //减缓存库存
        Long result = redisTemplate.opsForValue().increment
                ("promo_item_stock"+itemId, amount.intValue()*-1);

        if(result > 0){   //更新成功

            /*//消息是否成功
            boolean mqResult = producter.asyncReduceStock(itemId, amount);

            //如果失败，则回滚，redis中的库存加回来
            if(!mqResult){
                redisTemplate.opsForValue().increment
                        ("promo_item_stock"+itemId, amount.intValue());
                return false;
            }*/
            return true;
        }

        else if(result == 0){  //表示库存已经售罄
            redisTemplate.opsForValue().set("stock_sold_out"+itemId, true);
            return true;
        }

        //更新失败,回滚，redis中的库存加回来
        else{
            redisTemplate.opsForValue().increment
                    ("promo_item_stock"+itemId, amount.intValue());
            return false;
        }
    }



    @Override
    @Transactional
    public boolean increaseSales(Integer itemId, Integer amount) throws Exception {
        int affectedRow = itemDOMapper.increaseSales(itemId, amount);
        if(affectedRow > 0) return true;
        else return false;
    }

    //初始化库存流水
    @Override
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDO.setStatus(1); //1表示初始状态，2表示成功，3表示回滚
        stockLogDOMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }
}
