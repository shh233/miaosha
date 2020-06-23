package com.shh.miaosha.service.model;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ItemModel implements Serializable {

    private Integer id;

    @NotNull(message = "商品名称不能为空")
    private String title;

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格必须0")
    private BigDecimal price;

    @NotNull(message = "商品库存不能为空")
    private Integer stock;

    @NotNull(message = "商品描述不能为空")
    private String description;

    private Integer sales;

    @NotNull(message = "商品图片URL不能为空")
    private String imgUrl;

    private PromoModel promoModel;
}
