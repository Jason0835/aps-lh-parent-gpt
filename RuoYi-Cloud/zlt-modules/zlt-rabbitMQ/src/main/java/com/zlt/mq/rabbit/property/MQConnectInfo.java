package com.zlt.mq.rabbit.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties(prefix = "rabbit-mq")
@Getter
@Setter
@Component
public class MQConnectInfo {

    /**
     * 测试队列
     */
    private String queueDefault = "QUEUE_TEST_HELLO";

    /**
     * 死信交换机
     **/
    private String sysOrderDelayExchange = "SYS_ORDER_DELAY_EXCHANGE";
    /**
     * 接收死信队列消息
     **/
    private String sysOrderReceiveExchange = "SYS_ORDER_RECEIVE_EXCHANGE";
    /**
     * 延时队列
     **/
    private String sysOrderReceiveQueue = "SYS_ORDER_RECEIVE_QUEUE";

    /**
     * 死信接收队列
     **/
    private String sysOrderDelayQueue = "SYS_ORDER_DELAY_QUEUE";
    /**
     * 路由key
     **/
    private String sysOrderReceiveKey = "SYS_ORDER_RECEIVE_KEY";
    /**
     * 死信队列路由key
     **/
    private String sysOrderDelayKey = "SYS_ORDER_DELAY_KEY";

    /**
     * 记录每条发送数据到数据库的队列
     */
    private String sysSendLogDBQueue = "SYS_SEND_LOG_DB_QUEUE";

    /**
     * 失败消息记录到数据库的队列
     */
    private String sysFailMsgDBQueue = "SYS_FAIL_MSG_DB_QUEUE";
}
