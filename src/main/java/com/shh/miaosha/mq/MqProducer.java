/*
package com.shh.miaosha.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    private String nameServerAdd = "112.124.20.167:9876";
    private String topicName = "stock";


    //@PostConstruct 用作初始化，在这个bean加载之后就会初始化这个方法
    @PostConstruct
    public void init() throws Exception{

        //实例化消息生产者
        producer = new DefaultMQProducer("producer_group");

        //指定Nameserver的地址
        producer.setNamesrvAddr(nameServerAdd);

        //启动producer实例
        producer.start();

    }

    //同步库存扣减消息
    public boolean asyncReduceStock(Integer itemId, Integer amount){

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);

        //创建消息
        Message msg = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        try {
            //将消息投放出去
            producer.send(msg);
            System.out.println("消息投放成功");

        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;


    }

}
*/
