package com.shh.miaosha.service.model;

import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PromoModel implements Serializable {

    private Integer id;
    private String promoName;
    private DateTime startDate;
    private DateTime endDate;
    private Integer itemID;
    private BigDecimal promoItemPrice;
    private Integer status; //秒杀状态，1表示即将开始秒杀，2表示秒杀正在进行中，3表示秒杀已经结束

}
