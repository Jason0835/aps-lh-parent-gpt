package com.zlt.mq.rabbit.listener;

import com.rabbitmq.client.Channel;
import com.zlt.mq.rabbit.service.IDefaultMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 接收消息监听类
 *
 * @author japhet_jiu
 * @version 1.0
 */
@Service
@Slf4j
@RabbitListener(queues = "#{mqConnectInfo.sysOrderReceiveQueue}")
public class DelayReceiveMsgListener extends AbsReceiveMsgListener {

    @Resource(name="simpleMQServiceImpl")
    IDefaultMQService iDefaultMQService;
    /**
     * 获取到的延时消息
     * 这里接收到消息进行对应的业务处理(例如:登录该程序，进行一个短信通知)
     */
    @Override
    protected void mainProcess(String messageStr, Message message, Channel channel) throws Exception {

        iDefaultMQService.delayMQProcess(messageStr, message, channel);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
