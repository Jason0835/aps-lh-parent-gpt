package com.zlt.mix.common.engine.service.impl;

import com.alibaba.fastjson.JSON;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.domain.AutoScheduleLog;
import com.zlt.mix.common.engine.service.AutoScheduleLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * 自动排程日志Service业务层处理
 * 
 * @author zlt
 * @date 2021-07-16
 */
@Service
@Slf4j
public class AutoScheduleLogServiceImpl implements AutoScheduleLogService
{
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Value("${mq.scheduleLog.exchange}")
    private String exchange;
    @Value("${mq.scheduleLog.rutekey}")
    private String rutekey;

    /**
     * 新增终炼母炼自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertGlueScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_GLUE, batchNo, orderNo, title, logDetail);
        autoScheduleLog.setBaseValue(null);
        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增硫磺辅料自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertMaterialScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_MATERIAL, batchNo, orderNo, title, logDetail);
        autoScheduleLog.setBaseValue(null);
        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

}
