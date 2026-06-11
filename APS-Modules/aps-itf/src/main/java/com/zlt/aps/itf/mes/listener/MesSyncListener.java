package com.zlt.aps.itf.mes.listener;

import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.zlt.aps.itf.mes.enums.MesInterfaceCodeEnum;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.sync.service.SyncDataMQService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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
    @Resource
    private RedissonClient redissonClient;

    private final static long DEFAULT_WAIT_TIME = 5L;

    private final static long DEFAULT_LEASE_TIME = 180L;

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
            if (mesInterfaceCodeEnum == null) {
                log.warn("队列没有匹配的业务处理器....");
                return;
            }
            //加锁，防止消息短时间内重复：业务类型 + 来源系统 + 数据版本 + 目标系统
            String lockKey = String.format("%s||%s||%s||%s", syncKey, syncDataLogs.getDataSys(), syncDataLogs.getDataVersion(), syncDataLogs.getDockSys());
            log.info(String.format("Redis Key:%s", lockKey));
            RLock lock = redissonClient.getLock(lockKey);
            boolean isLocked = false;
            try {
                // 尝试获取锁，单位秒
                isLocked = lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
                if (!isLocked) {
                    //提示信息
                    String failMessage = String.format("业务：%s,已经在处理....", lockKey);
                    log.warn(failMessage);
                    throw new RuntimeException(failMessage);
                }
                log.info(String.format("获取到业务:%s 执行锁", lockKey));
                // 执行业务-从接口配置枚举获取处理类
                Object beanObj = SpringUtils.getBean(mesInterfaceCodeEnum.getServiceName());
                // 从接口配置枚举获取处理方法
                String methodName = mesInterfaceCodeEnum.getMethodName();
                // 接口处理类入参，统一为AuxReqSyncDataLogs
                Object[] params = new Object[]{syncDataLogs};
                ReflectUtils.invokeMethodByName(beanObj, methodName, params);
            } finally {
                // 释放锁
                if (isLocked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
