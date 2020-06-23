package com.shh.miaosha.mq;

import com.alibaba.fastjson.JSON;
import com.shh.miaosha.dao.StockLogDOMapper;
import com.shh.miaosha.dataobject.StockLogDO;
import com.shh.miaosha.service.OrderService;
import com.shh.miaosha.service.model.OrderModel;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Component
public class TransactionMqProducer {

    private TransactionMQProducer transactionMQProducer;

    private String nameServerAdd = "112.124.20.167:9876";
    private String topicName = "stock";


    @Autowired
    private OrderService orderService;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    //@PostConstruct 用作初始化，在这个bean加载之后就会初始化这个方法
    @PostConstruct
    public void init() throws Exception{

        //实例化基于事务的消息生产者
        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameServerAdd);

        //设置监听
        transactionMQProducer.setTransactionListener(new TransactionListener() {

            //发送消息之后，首先会回调该函数，我们将订单的创建放在此处，监听订单创建是否成功
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
                //参数里的arg就是发送消息中的argMap
                Integer userId = (Integer)((Map)arg).get("userId");
                Integer itemId = (Integer)((Map)arg).get("itemId");
                Integer amount = (Integer)((Map)arg).get("amount");
                String stockLogId = (String)((Map)arg).get("sotckLogId");

                //System.out.println(userId+" "+itemId+" "+amount);

                try {
                    //创建订单，缓存redis减库存
                    //System.out.println("创建订单");
                    OrderModel orderModel = orderService.createOrder(userId, itemId, amount,stockLogId);
                } catch (Exception e) {
                    e.printStackTrace();

                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            //由于RocketMQ迟迟没有收到消息的确认消息，因此主动询问这条消息，是否正常？
            // 可以查询数据库看这条数据是否已经处理
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String jsonString  = new String(msg.getBody());
                Map<String, Object> bodyMap =  JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer)bodyMap.get("itemId");
                Integer amount = (Integer)bodyMap.get("amount");
                String stockLogId = (String)bodyMap.get("sotckLogId");

                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                if(stockLogDO.getStatus() == 1){
                    return LocalTransactionState.UNKNOW;
                }
                else if(stockLogDO.getStatus() == 2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                else return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        });

        transactionMQProducer.start();

    }

    //基于事务的同步库存扣减消息
    public boolean transactionAsyncReduceStock(Integer userId, Integer itemId, Integer amount, String sotckLogId){

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("sotckLogId", sotckLogId);

        Map<String, Object> argMap = new HashMap<>();
        argMap.put("userId",userId);
        argMap.put("itemId",itemId);
        argMap.put("amount",amount);
        argMap.put("sotckLogId", sotckLogId);


        //创建消息
        Message msg = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        TransactionSendResult sendResult = null;

        try {
            //这里的参数arg会传到 TransactionListener的executeLocalTransaction方法中
            sendResult = transactionMQProducer.sendMessageInTransaction(msg, argMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }

        if(sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE){
            return false;
        }
        else if(sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE){
            return true;
        }
        return false;
    }

}
