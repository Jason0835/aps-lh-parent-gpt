package com.zlt.aps.common.engine.service.impl;

import com.alibaba.fastjson.JSON;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.constants.EngineMqConstant;
import com.zlt.aps.common.engine.domain.AutoScheduleLog;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
     * 新增硫化自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertLhScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_LH, batchNo, orderNo, title, logDetail);
        autoScheduleLog.setBaseVale(null);
        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增成型自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertCxScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_CX, batchNo, orderNo, title, logDetail);
        autoScheduleLog.setBaseVale(null);
        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增胎面自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertTmScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_TM, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增胎侧自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertTcScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_TC, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增内衬自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertNcScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_NC, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增胎圈自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertTqScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_TQ, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增钢丝圈自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertGsqScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_GSQ, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增15度裁断自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertCd15ScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_CD15, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增90度裁断自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertCd90ScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_CD90, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增钢带压延自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertGdyyScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_GDYY, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增纤维压延自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertXwyyScheduleLog(String batchNo, String orderNo, String title, String logDetail) {
//        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(EngineConstants.PROCEDURE_CODE_XWYY, batchNo, orderNo, title, logDetail);
//        autoScheduleLog.setBaseVale(null);
//        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

    /**
     * 新增自动排程日志
     * @param procedureCode 工序类型：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param title  标题
     * @param logDetail  日志内容
     */
    public void insertScheduleLog(String procedureCode, String batchNo, String orderNo, String title, String logDetail) {
        AutoScheduleLog autoScheduleLog = new AutoScheduleLog(procedureCode, batchNo, orderNo, title, logDetail);
        autoScheduleLog.setBaseVale(null);
        rabbitTemplate.convertAndSend(exchange, rutekey, JSON.toJSONString(autoScheduleLog));
    }

//    /**
//     * 新增自动排程日志
//     * @param procedureCode 工序类型：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延
//     * @param batchNo  批次号
//     * @param orderNo  工单号
//     * @param title  标题
//     * @param title  标题
//     * @param logDetail  日志内容
//     */
//   public void insertScheduleLog(String procedureCode, String batchNo, String orderNo, String title, String logDetail) {
//       AutoScheduleLog autoScheduleLog = new AutoScheduleLog(procedureCode, batchNo, orderNo, title, logDetail);
//       autoScheduleLog.setBaseVale(null);
//       autoScheduleLogMapper.insertAutoScheduleLog(autoScheduleLog);
//   }
}
