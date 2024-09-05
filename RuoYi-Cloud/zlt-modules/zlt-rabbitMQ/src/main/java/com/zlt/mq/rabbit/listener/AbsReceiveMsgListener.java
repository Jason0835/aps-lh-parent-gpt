package com.zlt.mq.rabbit.listener;

import com.rabbitmq.client.Channel;
import com.zlt.mq.rabbit.property.MQConnectInfo;
import com.zlt.mq.rabbit.service.MsgSenderService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;

import java.io.IOException;
import java.util.Map;

/**
 * 接收消息监听类
 *
 * @author japhet_jiu
 * @version 1.0
 */
//TODO:I18N
@EnableRabbit
@Slf4j
@Setter
@Getter
public abstract class AbsReceiveMsgListener {

    private Boolean isSave2DB = true;
    private Boolean isFail2Delay = true;
    private Boolean isFail2DB = true;

    @Autowired
    MsgSenderService msgSenderService;

    @Autowired
    MQConnectInfo mqConnectInfo;

    /**
     * 接收到的消息，保存消息到数据库，发送到指定队列
     * ，由队列处理器单独入库，或者做其它处理
     */
    protected void saveMsg2DB(Message message) {
        log.debug("记录消息到数据库,转入记录队列等待：{}", message);

        Message msgClone = getCloneMessage(message, mqConnectInfo.getSysSendLogDBQueue());
        try {
            msgSenderService.sendMsg(msgClone);
        } catch (Exception ex) {
            log.error("记录发送队列异常，被舍弃。{},ex:{}", message, ex);
        }
    }

    private Message getCloneMessage(Message message, String queue) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(queue);
        properties.getHeaders().putAll(message.getMessageProperties().getHeaders());

        return new Message(message.getBody(), properties);
    }

    protected void send2FailQueue(Message message) {
        log.error("处理消息失败,转入死信消息队列：{}", message);
        Message msgClone = getCloneMessage(message, mqConnectInfo.getSysOrderDelayQueue());
        try {
            msgSenderService.sendMsg(msgClone);
        } catch (Exception ex) {
            log.error("记录发送转入死信消息队列异常，被舍弃。{}", message, ex);
        }
    }

    protected void saveFail2DB(Message message, Exception ex) {
        log.info("处理消息失败,转入保存数据库队列：{},ex:{}", message, ex);
        Message msgClone = getCloneMessage(message, mqConnectInfo.getSysFailMsgDBQueue());
        try {
            msgSenderService.sendMsg(msgClone);
        } catch (Exception err) {
            log.error("记录发送转入保存数据库队列异常，被舍弃。{}", message, ex);
        }
    }

    protected void simpleProcess(String messageStr,
                                 Map<String, Object> headers,
                                 Message message,
                                 Channel channel) throws IOException {
        try {
            if (getIsSave2DB()) {
                saveMsg2DB(message);
            }
            mainProcess(messageStr, message, channel);

        } catch (Exception ex) {

            if (getIsFail2Delay()) {
                send2FailQueue(message);
            }
            if (getIsFail2DB()) {
                saveFail2DB(message, ex);
            }
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    @RabbitHandler
    protected void simpleMainProcess(byte[] bytes,
                                     @Headers Map<String, Object> headers,
                                     Message message,
                                     Channel channel) throws IOException {
        simpleProcess(new String(bytes), headers, message, channel);
    }

    @RabbitHandler
    protected void simpleMainProcess(String messageStr,
                                     @Headers Map<String, Object> headers,
                                     Message message,
                                     Channel channel) throws IOException {
        simpleProcess(messageStr, headers, message, channel);
    }

    /***
     * 派生类处理方法
     * @param message 消息包
     */
    protected abstract void mainProcess(String messageStr, Message message, Channel channel) throws Exception;

}
