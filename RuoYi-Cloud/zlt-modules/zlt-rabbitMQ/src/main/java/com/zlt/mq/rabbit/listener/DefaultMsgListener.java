package com.zlt.mq.rabbit.listener;

import com.rabbitmq.client.Channel;
import com.zlt.mq.rabbit.service.IDefaultMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 接收消息监听类
 *
 * @author japhet_jiu
 * @version 1.0
 */
@Service
@Slf4j
@RabbitListener(queues = "#{mqConnectInfo.queueDefault}")
public class DefaultMsgListener extends AbsReceiveMsgListener {

    @Resource(name = "simpleMQServiceImpl")
    IDefaultMQService iDefaultMQService;

    /**
     * 监听队列的消息
     */
    @Override
    protected void mainProcess(String messageStr, Message message, Channel channel) throws Exception {
        iDefaultMQService.defaultMQProcess(messageStr, message, channel);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
