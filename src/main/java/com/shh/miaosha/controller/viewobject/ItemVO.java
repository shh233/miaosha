package com.shh.miaosha.controller.viewobject;

import com.shh.miaosha.service.model.PromoModel;
import lombok.Data;

import javax.validation.constraints.Min;
import java.math.BigDecimal;

@Data
public class ItemVO {
    private Integer id;

    private String title;

    private BigDecimal price;

    private Integer stock;

    private String description;

    private Integer sales;

    private String imgUrl;

    private Integer promoStatus;

    private BigDecimal promoPrice;

    private Integer promoId;

    private String startDate;
}
