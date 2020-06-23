/*
package com.shh.miaosha.mq;

import com.alibaba.fastjson.JSON;
import com.shh.miaosha.dao.ItemStockDOMapper;
import com.shh.miaosha.service.ItemService;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Component
public class MqConsumer {

    private DefaultMQPushConsumer consumer;

    private String nameServerAdd = "112.124.20.167:9876";
    private String topicName = "stock";

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    //@PostConstruct 用作初始化，在这个bean加载之后就会初始化这个方法
    @PostConstruct
    public void init() throws Exception{
        //实例化消费者
        consumer = new DefaultMQPushConsumer("stock_consumer_group");

        //设置NameServer的地址
        consumer.setNamesrvAddr(nameServerAdd);

        //订阅一个或者多个Topic，以及Tag ，来过滤需要消费的消息
        consumer.subscribe(topicName, "*");

        //注册回调实现类来处理从broker拉取回来的消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {

                //消费消息
                Message msg = list.get(0);
                String jsonString  = new String(msg.getBody());
                //System.out.println("jsonString:"+jsonString);
                Map<String, Object> map =  JSON.parseObject(jsonString, Map.class);
                //System.out.println("map"+map.size());
                Integer itemId = (Integer)map.get("itemId");
                Integer amount = (Integer)map.get("amount");

                //System.out.println("itemId"+itemId+"amount"+amount);

                //实现数据库真正的扣减操作
                itemStockDOMapper.decreaseStock(itemId, amount);

                System.out.println("消息消费成功");
                //消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        //启动消费者
        consumer.start();


    }


}
*/
