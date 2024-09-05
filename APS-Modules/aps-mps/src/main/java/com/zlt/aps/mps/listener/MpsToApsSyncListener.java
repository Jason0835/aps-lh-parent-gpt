package com.zlt.aps.mps.listener;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.TSyncMps2ApsFac;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.mps.common.ServiceTypeEnum;
import com.zlt.aps.mps.domain.MonthPlanSyncVo;
import com.zlt.aps.mps.domain.TServiceSyncLog;
import com.zlt.aps.mps.service.MesSyncLogService;
import com.zlt.aps.mps.service.MonthPlanSumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主计划同步
 */
@Slf4j
@EnableRabbit
@Component
public class MpsToApsSyncListener {

    @Autowired
    private MonthPlanSumService monthPlanSumService;
    @Autowired
    private ISysConfigService configService;
    @Autowired
    private MesSyncLogService logService;

    private static String division = "\r\n---------------------------------------------------\r\n";// 日志分割符
    /**
     * 主计划分厂版本下发同步
     * @param data
     * @param queue
     */
    @RabbitListener(bindings = {@QueueBinding(exchange = @Exchange(value = "${mq.mpsPlanMain.exchange}", autoDelete = "false"), key = "${mq.mpsPlanMain.rutekey}",
            value = @Queue(value = "${mq.mpsPlanMain.queue}"))
    }, ackMode = "AUTO")
    public void facSyncProcessor(String data, @Header(AmqpHeaders.CONSUMER_QUEUE) String queue) {
        try {
            log.info("同步开始，" + "时间：" + DateUtil.now() + division);
            TServiceSyncLog syncLog = new TServiceSyncLog();
            syncLog.setCreateTime(DateUtil.now());
            syncLog.setUpdateTime(DateUtil.now());
            syncLog.setServiceType(ServiceTypeEnum.REQUEST.ordinal() + "");
            syncLog.setServiceStatus("0");
            String isOpen = configService.selectConfigByKey("MPS_TO_APS_RECEIVE_SWITCH");
            if (isOpen != null && StringUtils.isNotEmpty(isOpen) && isOpen.equals("ON")) {
                MonthPlanSyncVo entity = JSON.parseObject(data, MonthPlanSyncVo.class);
                entity.setIsFinal(entity.getIsFinal() == 0 ? 1 : 0);
                syncLog.setServiceParams("data:" + data);
                log.info("数据data：" + data + division);
                // 获取生产排程版本
                int exist = monthPlanSumService.checkMpsExist(entity.getYear(), entity.getMonth(), entity.getVersion());
                if (exist == 0) {
                    syncLog.setServiceStatus("1");
                    syncLog.setServiceResult("T_SYNC_MPS_2_APS_FAC表数据不存在：year:" + entity.getYear() + " month:" + entity.getMonth() + " dataVersion:" + entity.getVersion());
                } else {
                    // 月度计划汇总
                    AjaxResult result = monthPlanSumService.monthPlanAmountSum(entity.getVersion(), entity.getYear().toString(), entity.getMonth().toString(), entity.getIsFinal());
                    if ((int)result.get(Constants.CODE) == HttpStatus.ERROR) {
                        syncLog.setServiceStatus("1");
                        syncLog.setServiceResult((String) result.get(GatewayConstants.MSG_TAG) + division + "主计划同步失败");
                    } else {
                        syncLog.setServiceResult("主计划同步成功");
                    }
                }
            } else {
                syncLog.setServiceStatus("1");
                syncLog.setServiceResult("系统参数：MPS_TO_APS_RECEIVE_SWITCH不存在或者不为ON");
            }
            logService.addLog(syncLog);
            log.info("日志插入完成" + division);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                TServiceSyncLog syncLog = new TServiceSyncLog();
                syncLog.setCreateTime(DateUtil.now());
                syncLog.setUpdateTime(DateUtil.now());
                syncLog.setServiceType(ServiceTypeEnum.REQUEST.ordinal() + "");
                syncLog.setServiceStatus("1");
                syncLog.setServiceParams("data:" + data);
                syncLog.setServiceResult("主计划同步失败" + division + e.toString());
                logService.addLog(syncLog);
                log.info("日志插入完成" + division);
            } catch (Exception e2) {
                log.error(e.getMessage(), e2);
            }
        }
//        log.info("消息确认完成" + division);
    }
}
