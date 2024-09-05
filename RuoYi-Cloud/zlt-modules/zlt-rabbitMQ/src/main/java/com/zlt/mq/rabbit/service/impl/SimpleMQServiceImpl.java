package com.zlt.mq.rabbit.service.impl;

import com.rabbitmq.client.Channel;
import com.zlt.mq.rabbit.service.IDefaultMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("simpleMQServiceImpl")
@Slf4j
public class SimpleMQServiceImpl implements IDefaultMQService {

    @Override
    public void defaultMQProcess(String messageStr, Message message, Channel channel) throws Exception {
        log.info("---->接收到的消息:{}", messageStr);
    }

    @Override
    public void delayMQProcess(String messageStr, Message message, Channel channel) throws Exception {
        log.info("MQ接收消息时间:{},消息内容:{}", messageStr);
    }
}
