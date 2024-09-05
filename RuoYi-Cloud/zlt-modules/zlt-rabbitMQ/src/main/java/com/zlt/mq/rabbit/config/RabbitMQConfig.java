package com.zlt.mq.rabbit.config;

import com.zlt.mq.rabbit.property.MQConnectInfo;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置类
 *
 * @author japhet_jiu
 * @version 1.0
 */

@Configuration
public class RabbitMQConfig {

    @Bean
    public MQConnectInfo mqConnectInfo(){
        return new MQConnectInfo();
    }

    /**
     * 测试发送消息到MQ
     *
     * @return
     */
    @Bean
    public Queue defaultQueue() {
        return new Queue(mqConnectInfo().getQueueDefault());
    }

    /**
     * 死信交换机
     *
     * @return
     */
    @Bean
    public DirectExchange sysOrderDelayExchange() {
        return new DirectExchange(mqConnectInfo().getSysOrderDelayExchange());
    }

    /**
     * 死信队列
     *
     * @return
     */
    @Bean
    public Queue sysOrderDelayQueue() {
        Map<String, Object> map = new HashMap<String, Object>(16);
        map.put("x-dead-letter-exchange", mqConnectInfo().getSysOrderReceiveExchange()); //指定死信送往的交换机
        map.put("x-dead-letter-routing-key", mqConnectInfo().getSysOrderReceiveKey()); //指定死信的routingkey
        return new Queue(mqConnectInfo().getSysOrderDelayQueue(), true, false, false, map);
    }

    /**
     * 给死信队列绑定死信交换机
     *
     * @return
     */
    @Bean
    public Binding sysOrderDelayBinding() {
        return BindingBuilder.bind(sysOrderDelayQueue()).to(sysOrderDelayExchange()).with(mqConnectInfo().getSysOrderDelayKey());
    }

    /**
     * 死信接收交换机,用于接收死信队列的消息
     *
     * @return
     */
    @Bean
    public DirectExchange sysOrderReceiveExchange() {
        return new DirectExchange(mqConnectInfo().getSysOrderReceiveExchange());
    }

    /**
     * 死信接收队列
     *
     * @return
     */
    @Bean
    public Queue sysOrderReceiveQueue() {
        return new Queue(mqConnectInfo().getSysOrderReceiveQueue());
    }

    /**
     * 死信接收交换机绑定接收死信队列消费队列
     *
     * @return
     */
    @Bean
    public Binding sysOrdeReceiveBinding() {
        return BindingBuilder.bind(sysOrderReceiveQueue()).to(sysOrderReceiveExchange()).with(mqConnectInfo().getSysOrderReceiveKey());
    }

    /**
     * 记录每条发送数据到数据库的队列
     * @return
     */
    @Bean
    public Queue sysSendLogDBQueue() {
        return new Queue(mqConnectInfo().getSysSendLogDBQueue());
    }

    /**
     * 失败消息记录到数据库的队列
     * @return
     */
    @Bean
    public Queue sysFailMsgDBQueue() {
        return new Queue(mqConnectInfo().getSysFailMsgDBQueue());
    }
}
