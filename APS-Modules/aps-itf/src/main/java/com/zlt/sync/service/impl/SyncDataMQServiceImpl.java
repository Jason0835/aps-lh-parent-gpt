package com.zlt.sync.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.sync.constants.SyncConstants;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import com.zlt.sync.domain.AuxReqSyncDataLogsHis;
import com.zlt.sync.mapper.AuxReqSyncDataLogsHisMapper;
import com.zlt.sync.mapper.AuxReqSyncDataLogsMapper;
import com.zlt.sync.povo.SyncDataVO;
import com.zlt.sync.service.SyncDataMQService;
import com.zlt.sync.utils.SpringBeanUtils;

@Primary
@Service("syncDataMQService")
public class SyncDataMQServiceImpl implements SyncDataMQService {
    private static final Logger logger = LoggerFactory.getLogger(SyncDataMQServiceImpl.class);

    @Autowired
    private AuxReqSyncDataLogsMapper auxReqSyncDataLogsMapper;

    @Autowired
    private AuxReqSyncDataLogsHisMapper auxReqSyncDataLogsHisMapper;

    @Autowired
    private SyncConstants syncConstants;

    /**
     * 如果存在 msgId，需要比对 SYNC_KEY, DATA_SYS, DOCK_SYS (当消息体有存在该字段时)
     * 如果不存在 msgId，需要补充 DATA_SYS, DOCK_SYS, BACK_ISSUE  (SYNC_KEY一定要有)
     * @param messageStr
     * @throws Exception
     */
    public AuxReqSyncDataLogs handleMQProcess(String messageStr) {
    	AuxReqSyncDataLogs dataLogs = new AuxReqSyncDataLogs();
        logger.info("defaultMQProcess-001 接收同步反馈消息, 消息内容: " + messageStr);

        if (StringUtils.isEmpty(messageStr)) {
            logger.info("defaultMQProcess-002 接收同步反馈消息, 消息内容: 为空");
            return dataLogs;
        }

        try {
            dataLogs = JSONObject.parseObject(messageStr, AuxReqSyncDataLogs.class);

            if (dataLogs == null) {
                logger.error("defaultMQProcess-003 接收同步反馈消息, 消息格式错误;");
                return dataLogs;
            }

            if (StringUtils.isNull(dataLogs.getMsgId()) || StringUtils.isEmpty(dataLogs.getMsgId())) {
                logger.error("defaultMQProcess-004 接收同步反馈消息，反馈数据异常，没有包括 msgId 主键值;");
                return dataLogs;
            }

            if (StringUtils.isNull(dataLogs.getStatus())) {
                logger.error("defaultMQProcess-005 接收同步反馈消息: 没有反馈消息状态 status");
                return dataLogs;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("msgId", dataLogs.getMsgId());
            List<AuxReqSyncDataLogs> dataLogsDbs = auxReqSyncDataLogsMapper.queryReqSyncDataLogs(params);

            AuxReqSyncDataLogs dataLogsDb = new AuxReqSyncDataLogs();

            if (CollectionUtils.isEmpty(dataLogsDbs)) {
                logger.warn("defaultMQProcess-0051 接收同步反馈消息: 本地中间库没有msgId: " + dataLogs.getMsgId() + "; 数据; (对接系统主动推送消息)");

                if (StringUtils.isEmpty(dataLogs.getSyncKey())) {
                    logger.error("defaultMQProcess-00511 接收同步反馈消息: SyncKey不存在; ");
                    return dataLogs;
                }

                SyncDataVO dataVO = syncConstants.getSyncDataByKey(dataLogs.getSyncKey());
                if (dataVO == null) {
                    logger.error("defaultMQProcess-00511 接收同步反馈消息: SyncKey 对应的 接口配置不存在; " + dataLogs.getSyncKey());
                    return dataLogs;
                }

                dataLogs.setDataSys(dataVO.getDataSys());
                dataLogs.setDockSys(dataVO.getDockSys());
                dataLogs.setBackIssue(dataVO.getBackIssue());

                SpringBeanUtils.copyPropertiesIgnoreNull(dataLogs, dataLogsDb);
                dataLogsDb.setCreateDate(new Date());
                dataLogsDb.setUpdateDate(new Date());

                auxReqSyncDataLogsMapper.insert(dataLogsDb);
            } else {
                dataLogsDb = dataLogsDbs.get(0);

                if (!StringUtils.isEmpty(dataLogs.getSyncKey()) && !dataLogs.getSyncKey().equals(dataLogsDb.getSyncKey())) {
                    logger.error("defaultMQProcess-0051 接收同步反馈消息: 消息与源状态SyncKey不一致: " + dataLogs.getSyncKey() + "_" + dataLogsDb.getSyncKey());
                    return dataLogs;
                }

                if (!StringUtils.isEmpty(dataLogs.getDockSys()) && !dataLogs.getDockSys().equals(dataLogsDb.getDockSys())) {
                    logger.error("defaultMQProcess-0052 接收同步反馈消息: 消息与源状态DockSys不一致: " + dataLogs.getDockSys() + "_" + dataLogsDb.getDockSys());
                    return dataLogs;
                }

                if (!StringUtils.isEmpty(dataLogs.getDataSys()) && !dataLogs.getDataSys().equals(dataLogsDb.getDataSys())) {
                    logger.error("defaultMQProcess-0053 接收同步反馈消息: 消息与源状态DataSys不一致: " + dataLogs.getDataSys() + "_" + dataLogsDb.getDataSys());
                    return dataLogs;
                }

                SpringBeanUtils.copyPropertiesIgnoreNull(dataLogs, dataLogsDb);
                dataLogs.setUpdateDate(new Date());
                auxReqSyncDataLogsMapper.update(dataLogsDb);
            }

            // 插入历史 记录
            AuxReqSyncDataLogsHis logsHis = new AuxReqSyncDataLogsHis();
            SpringBeanUtils.copyPropertiesIgnoreNull(dataLogsDb, logsHis);
            logsHis.setMsgId(UUID.randomUUID().toString());
            logsHis.setCreateDate(new Date());
            logsHis.setUpdateDate(logsHis.getCreateDate());
            auxReqSyncDataLogsHisMapper.insert(logsHis);

            logger.info("defaultMQProcess-006 接收同步反馈消息: 状态及历史记录更新添加成功");
        } catch (Exception ex) {
            logger.error("defaultMQProcess-007 接收同步反馈消息， 操作异常: " + ex.getMessage(), ex);
        }
        return dataLogs;
    }
}
