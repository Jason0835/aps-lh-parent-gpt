package com.zlt.aps.common.engine.listener;

import com.alibaba.fastjson.JSON;
import com.zlt.aps.common.engine.constants.EngineMqConstant;
import com.zlt.aps.common.engine.domain.AutoScheduleLog;
import com.zlt.aps.common.engine.mapper.AutoScheduleLogMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
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
     * 插入排程日志
     * @param data
     * @param queue
     */
    @RabbitListener(bindings = {@QueueBinding(exchange = @Exchange(value = "${mq.scheduleLog.exchange}", autoDelete = "true"), key = "${mq.scheduleLog.rutekey}",
            value = @Queue(value = "${mq.scheduleLog.queue}"))
    })
    public void autoScheduleLogProcessor(String data, @Header(AmqpHeaders.CONSUMER_QUEUE) String queue) {
        try {
            AutoScheduleLog log = JSON.parseObject(data, AutoScheduleLog.class);
            autoScheduleLogMapper.insertAutoScheduleLog(log);
        } catch (Exception e) {
            log.error("插入排程日志异常", e);
            log.error("排程日志异常参数为：{}", data);
        }
    }
}
