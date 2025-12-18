package com.zlt.aps.itf.mes.listener;

import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.zlt.aps.itf.mes.enums.MesInterfaceCodeEnum;
import com.zlt.aps.itf.mes.service.impl.MesItfServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author Chen
 * @since 2025/12/16
 */
@Slf4j
@EnableRabbit
@Component
public class MesSyncListener {

    /**
     * 接收MES消息
     *
     * @param data  数据
     * @param queue 队列
     */
    @RabbitListener(bindings = {
            @QueueBinding(exchange =
            @Exchange(value = "${syncdata.sendQueues.MES_APS.exchange}"),
                    key = "${syncdata.sendQueues.MES_APS.key}",
                    value = @Queue(value = "${syncdata.sendQueues.MES_APS.queue}")
            )
    }, ackMode = "AUTO")
    public void processor(String data, @Header(AmqpHeaders.CONSUMER_QUEUE) String queue) {
        try {
            MesInterfaceCodeEnum mesInterfaceCodeEnum = MesInterfaceCodeEnum.getByCode(data);
            if (mesInterfaceCodeEnum != null) {
                ReflectUtils.invokeMethodByName(MesItfServiceImpl.class, mesInterfaceCodeEnum.getMethodName(), new Object[]{new Object()});
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
