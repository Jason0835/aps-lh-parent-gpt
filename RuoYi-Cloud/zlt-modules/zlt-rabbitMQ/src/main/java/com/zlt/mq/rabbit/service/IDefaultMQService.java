package com.zlt.mq.rabbit.service;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;


public interface IDefaultMQService {

    void defaultMQProcess(String messageStr, Message message, Channel channel) throws Exception;

    void delayMQProcess(String messageStr, Message message, Channel channel) throws Exception;
}
