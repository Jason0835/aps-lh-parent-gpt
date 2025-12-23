package com.zlt.sync.worker.processor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.sync.constants.SyncConstants;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import com.zlt.sync.povo.SyncDataVO;
import com.zlt.sync.worker.IProcessor;
import com.zlt.sync.worker.Task;

/**
 * 同步失败任务重试
 */
@Component
public class SyncDataReTryeProcessor implements IProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SyncDataReTryeProcessor.class);

    @Autowired
    private SyncConstants syncConstants;

    /**
     * 有一个失败，后续相似转换，取消
     * 同步后，有的成功，有的失败，需求系统 + status=2的数据，全部捞出来给前端, 前端处理后，要将数据状态改为 6
     * 如果 没有处理 status=2 至 6这个阶段，下次还是会发给前端;
     * @param task
     */
    @Override
    public void handle(Task task) {
        logger.info("SyncDataReTryeProcessor-001 执行可同步数据提交执行;");

        List<AuxReqSyncDataLogs> syncList = task.getSyncList();

        String errorSyncKey = null;
        for (AuxReqSyncDataLogs dataLogs : syncList) {

            if(!StringUtils.isEmpty(errorSyncKey) && errorSyncKey.equals(dataLogs.getSyncKey())) {
                logger.error("SyncDataReTryeProcessor-002 执行可同步数据提交执行; syncKey: " + dataLogs.getSyncKey() + ", 正在同步中, 或已发生同步错误(看下采集平台监控管理或服务器日志)");
                continue;
            }

            logger.info("SyncDataReTryeProcessor-003 执行可同步数据提交执行; syncKey: " + dataLogs.getSyncKey());

            SyncDataVO dataVO = syncConstants.getSyncDataByKey(dataLogs.getSyncKey());

            if (dataVO == null || StringUtils.isEmpty(dataVO.getTaskId())) {
                logger.error("SyncDataReTryeProcessor-004 执行可同步数据提交执行; 获取任务信息异常, 无法获取 syncKey: " + dataLogs.getSyncKey() + " 的配置信息 (目的taskId)");
                continue;
            }
            logger.info("SyncDataReTryeProcessor-005 执行可同步数据提交执行; syncKey: "+ dataLogs.getSyncKey() +" 获取任务id: " + dataVO.getTaskId());

            AjaxResult result = null;
            try {
//                result = iProxyService.startTrans("1", Integer.parseInt(dataVO.getTaskId()));
                // TODO 触发同步接口，根据syncKey找到对应执行类
                dataLogs.getSyncKey();
            } catch (Exception ex) {
                logger.error("SyncDataReTryeProcessor-006 执行可同步数据提交, 调用异常: " + ex.getMessage(), ex);
            }

            if (result != null && result.containsKey(AjaxResult.CODE_TAG)) {
                int code = Integer.parseInt(result.get(AjaxResult.CODE_TAG).toString());
                if (code != 200) {
                    String msg = "";
                    if (result.containsKey(AjaxResult.MSG_TAG)) {
                        msg = result.get(AjaxResult.MSG_TAG).toString();
                    }
                    logger.error("SyncDataReTryeProcessor-007 执行可同步数据提交执行; 同步执行失败, syncKey: " + dataLogs.getSyncKey() + "; code: " + code + "; msg: "+ msg);
                    errorSyncKey = dataLogs.getSyncKey();
                    continue;
                }
            }

        } // for end
    }

}
