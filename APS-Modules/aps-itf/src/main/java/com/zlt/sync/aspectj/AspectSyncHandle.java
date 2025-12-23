package com.zlt.sync.aspectj;

import com.zlt.sync.domain.AuxReqSyncDataLogs;
import com.zlt.sync.domain.AuxReqSyncDataLogsHis;
import com.zlt.sync.mapper.AuxReqSyncDataLogsHisMapper;
import com.zlt.sync.mapper.AuxReqSyncDataLogsMapper;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncMsgSenderService;
import com.zlt.sync.utils.SpringBeanUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Date;
import java.util.UUID;

@Component
@Aspect
public class AspectSyncHandle {
    private static final Logger logger = LoggerFactory.getLogger(AspectSyncHandle.class);

    @Autowired
    private SyncMsgSenderService syncMsgSenderService;

    @Autowired
    private AuxReqSyncDataLogsMapper auxReqSyncDataLogsMapper;

    @Autowired
    private AuxReqSyncDataLogsHisMapper auxReqSyncDataLogsHisMapper;

    // 消息先于事务过来，导至找不到数据; 2022-01-07
    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Pointcut("@annotation(com.zlt.sync.aspectj.AopSyncData)")
    public void syncPoint() {}

    @Around("syncPoint()")
    public Object around(ProceedingJoinPoint joinPoint) {

        logger.info("AspectSyncHandle-around-001 同步数据请求或通知 发送MQ以及记录日志");
        Object object = null;

        try {
            object = joinPoint.proceed();
            Object[] args = joinPoint.getArgs();

            SyncParamsVO paramsVO = (SyncParamsVO) args[0];
            AuxReqSyncDataLogs dataLogs = (AuxReqSyncDataLogs) args[1];

            if (paramsVO == null || dataLogs == null) {
                logger.error("AspectSyncHandle-around-002 执行过程异常，入参结果为null");
                return object;
            }

            logger.info("AspectSyncHandle-around-003 同步数据请求或通知 发送MQ以及记录日志, SYNC_KEY: " + paramsVO.getSyncKey() + "; 是否发MQ: " + paramsVO.getNoMq());

            if (paramsVO.getParams() != null && !paramsVO.getParams().containsKey("version")) {
                paramsVO.getParams().put("version", dataLogs.getDataVersion());
            }

            String msgId = UUID.randomUUID().toString();
            dataLogs.setMsgId(msgId);
            paramsVO.setMsgId(msgId);
            dataLogs.setParams(paramsVO.getParams() != null ? paramsVO.getParams().toJSONString(): "{}");

            //=======================
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = dataSourceTransactionManager.getTransaction(def);
            //========================
            // 记录请求状态日志
            dataLogs.setUpdateDate(new Date());
            auxReqSyncDataLogsMapper.insert(dataLogs);

            logger.info("AspectSyncHandle-around-005 同步数据请求或通知 记录状态日志成功, SYNC_KEY: " + paramsVO.getSyncKey());

            // 记录请求历史日志
            AuxReqSyncDataLogsHis logsHis = new AuxReqSyncDataLogsHis();
            SpringBeanUtils.copyPropertiesIgnoreNull(dataLogs, logsHis);
            auxReqSyncDataLogsHisMapper.insert(logsHis);

            logger.info("AspectSyncHandle-around-006 同步数据请求或通知 记录历史记录成功, SYNC_KEY: " + paramsVO.getSyncKey());
            //===========================
            dataSourceTransactionManager.commit(status);
            //===========================

            // 首行发送参数消息给对接系统 (MQ)
            if (!Integer.valueOf(1).equals(paramsVO.getNoMq())) {
                syncMsgSenderService.send(paramsVO.toJSONString(), paramsVO.getDataSys(), paramsVO.getDockSys());
                logger.info("AspectSyncHandle-around-004 同步数据请求或通知 发送MQ消息成功, SYNC_KEY: " + paramsVO.getSyncKey());
            }
        } catch (Throwable t) {
            logger.error("AspectSyncHandle-around-007 同步数据请求或通知 执行过程异常，异常: " + t.getMessage(), t);
        }

        return object;
    }
}
