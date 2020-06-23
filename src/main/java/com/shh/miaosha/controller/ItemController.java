package com.shh.miaosha.controller;

import com.shh.miaosha.Response.CommonReturnType;
import com.shh.miaosha.controller.viewobject.ItemVO;
import com.shh.miaosha.dataobject.ItemDO;
import com.shh.miaosha.service.CacheService;
import com.shh.miaosha.service.ItemService;
import com.shh.miaosha.service.PromoService;
import com.shh.miaosha.service.model.ItemModel;
import com.shh.miaosha.service.model.PromoModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true" , allowedHeaders = "*")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;


    @RequestMapping(value = "/create" , method = {RequestMethod.POST} , consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "imgUrl") String imgUrl,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "stock") Integer stock
                                       ) throws Exception {
        ItemModel itemModel = new ItemModel();
        itemModel.setStock(stock);
        itemModel.setPrice(price);
        itemModel.setDescription(description);
        itemModel.setImgUrl(imgUrl);
        itemModel.setTitle(title);


        ItemModel returnItemModel = itemService.createItem(itemModel);
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(returnItemModel, itemVO);
        System.out.println("VO:"+itemVO.getId());
        return CommonReturnType.create(itemVO);
    }

    @RequestMapping(value = "/publishpromo" , method = {RequestMethod.GET})
    public CommonReturnType publishPromo(@RequestParam(name = "promoId") Integer promoId) throws Exception {

        promoService.publishPromo(promoId);
        return CommonReturnType.create(null);
    }


    @RequestMapping(value = "/get" , method = {RequestMethod.GET})
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) throws Exception{

        ItemModel itemModel = new ItemModel();

        //从本地热点缓存中取
        itemModel = (ItemModel) cacheService.getCommonCache("item_"+id);

        if(itemModel == null){

            //从redis中获取ItemModel
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+id);

            //否则从数据库中查询
            if(itemModel == null){
                itemModel = itemService.getItemById(id);
                redisTemplate.opsForValue().set("item_"+id, itemModel);
                redisTemplate.expire("item_"+id, 1, TimeUnit.MINUTES);
            }

            cacheService.setCommonCache("item_"+id, itemModel);
        }


        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        PromoModel promoModel = itemModel.getPromoModel();
        //System.out.println("promoModel:"+promoModel);
        if(promoModel != null){
            itemVO.setPromoStatus(promoModel.getStatus());
            itemVO.setPromoPrice(promoModel.getPromoItemPrice());
            itemVO.setPromoId(promoModel.getId());
            itemVO.setStartDate(promoModel.getStartDate().toString("yyyy-MM-dd HH:mm:ss"));
        }
        else{
            itemVO.setPromoStatus(0);
        }
        return CommonReturnType.create(itemVO);
    }

    @RequestMapping(value = "/list" , method = {RequestMethod.GET})
    public CommonReturnType getItemList(){
        List<ItemModel> list = itemService.listItem();
        List<ItemVO> resultList = new ArrayList<>();
        list.forEach(itemModel -> {
            ItemVO itemVO = new ItemVO();
            BeanUtils.copyProperties(itemModel, itemVO);
            resultList.add(itemVO);
        });
        return CommonReturnType.create(resultList);
    }
}
