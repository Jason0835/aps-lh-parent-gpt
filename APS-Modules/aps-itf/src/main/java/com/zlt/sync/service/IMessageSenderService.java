package com.zlt.sync.service;

/**
 * 各前端自己实现要发送到的交换机以及Key
 */
public interface IMessageSenderService {

    void send(String syncParams);
}
