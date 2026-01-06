package com.zlt.aps.itf.mes.listener;

import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.zlt.aps.itf.mes.enums.MesInterfaceCodeEnum;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.sync.service.SyncDataMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Chen
 * @since 2025/12/16
 */
@Slf4j
@EnableRabbit
@Component
public class MesSyncListener {
    @Resource(name = "syncDataMQService")
    private SyncDataMQService syncDataMQService;

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
        	AuxReqSyncDataLogs syncDataLogs = syncDataMQService.handleMQProcess(data);
            String syncKey = syncDataLogs.getSyncKey();
            MesInterfaceCodeEnum mesInterfaceCodeEnum = MesInterfaceCodeEnum.getByCode(syncKey);
            if (mesInterfaceCodeEnum != null) {
            	Object beanObj = SpringUtils.getBean(mesInterfaceCodeEnum.getServiceName()); // 从接口配置枚举获取处理类
            	String methodName = mesInterfaceCodeEnum.getMethodName(); // 从接口配置枚举获取处理方法
            	Object[] params = new Object[] {syncDataLogs}; // 接口处理类入参，统一为AuxReqSyncDataLogs
                ReflectUtils.invokeMethodByName(beanObj, methodName, params);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
