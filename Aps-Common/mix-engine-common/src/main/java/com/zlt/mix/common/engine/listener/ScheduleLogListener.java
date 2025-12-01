package com.zlt.mix.common.engine.listener;

import com.alibaba.fastjson.JSON;
import com.zlt.mix.common.engine.domain.AutoScheduleLog;
import com.zlt.mix.common.engine.mapper.AutoScheduleLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 排程日志监听（消费者）
 */
@Slf4j
@EnableRabbit
@Component
public class ScheduleLogListener {

    @Resource
    private AutoScheduleLogMapper autoScheduleLogMapper;
    /**
     * 排程引擎算法是否需要记录过程数据日志开关（需要：true，不需要：false）
     */
    @Value("${auto.schedule.logSwitch:true}")
    private String logSwitch;

    /**
     * 插入排程日志
     * @param data
     * @param queue
     */
    @RabbitListener(bindings = {@QueueBinding(exchange = @Exchange(value = "${mq.scheduleLog.exchange}", autoDelete = "true"), key = "${mq.scheduleLog.rutekey}",
            value = @Queue(value = "${mq.scheduleLog.queue}"))
    })
    public void autoScheduleLogProcessor(String data, @Header(AmqpHeaders.CONSUMER_QUEUE) String queue) {
        try {
            Boolean isOpen = Boolean.valueOf(logSwitch);  //获取自动排程算法引擎日志开关（false的话关闭，不插入日志）
            if(isOpen) {
                AutoScheduleLog log = JSON.parseObject(data, AutoScheduleLog.class);
                autoScheduleLogMapper.insertAutoScheduleLog(log);
            }
        } catch (Exception e) {
            log.error("插入排程日志异常", e);
            log.error("排程日志异常参数为：{}", data);
        }
    }
}
