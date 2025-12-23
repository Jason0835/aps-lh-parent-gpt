package com.zlt.sync.configuration;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zlt.sync.povo.QueueDataVO;

/**
 * 模拟接收消息, Default 接收
 */
@Configuration
public class SyncRabbitMqConfig {
    private static final Logger logger = LoggerFactory.getLogger(SyncRabbitMqConfig.class);

    @Autowired
    private QueueConfigDatas queueConfigDatas;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public Queue syncReceiveQueue() {
        //mqConnectInfo.setQueueDefault(queueConfigDatas.getReceiveQueue());
        logger.info("syncReceiveQueue-001: 侦听队列: " + queueConfigDatas.getReceiveQueue());
        return new Queue(queueConfigDatas.getReceiveQueue());
    }

    @Bean
    public void initSyncQueues() throws BeansException {

        ConfigurableApplicationContext context = (ConfigurableApplicationContext)applicationContext;
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)context.getBeanFactory();

//        if (queueConfigDatas == null) {
//            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(QueueConfigDatas.class);
//        }

        Map<String, QueueDataVO> queueDataMap = queueConfigDatas.getSendQueues();

        if (queueDataMap != null && queueDataMap.size() > 0) {

            for (String key : queueDataMap.keySet()) { //eg: APS_MES
                QueueDataVO queueDataVO = queueDataMap.get(key);

                // 创建 queue bean
                Queue queue = new Queue(queueDataVO.getQueue());

                // 创建 exchange bean
                DirectExchange exchange = new DirectExchange(queueDataVO.getExchange());

                // binding
                Binding binding = BindingBuilder.bind(queue).to(exchange).with(queueDataVO.getKey());

                // 注册bean
                beanFactory.registerSingleton(key + "_queue", queue);
                beanFactory.registerSingleton(key + "_exchange", exchange);
                beanFactory.registerSingleton(key + "_binding", binding);

                logger.info("postProcessBeanFactory-001 初始化Queue: " + key);
            }

        }
    }
}
