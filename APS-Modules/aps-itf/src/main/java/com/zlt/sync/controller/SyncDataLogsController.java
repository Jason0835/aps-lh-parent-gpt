package com.zlt.sync.controller;

import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.service.SyncDataLogsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 同步日志控制类
 *
 * @author Chen
 * @since 2025/12/24
 */
@RestController
@RequestMapping(value = "/syncDataLogs")
public class SyncDataLogsController {

    private static final Logger logger = LoggerFactory.getLogger(SyncDataLogsController.class);
    
    @Autowired
    private SyncDataLogsService syncDataLogsService;

    /**
     * 获取数据版本
     *
     * @param syncKey 同步标识
     * @return 数据版本号
     */
    @GetMapping("/getDataVersion/{syncKey}")
    public String getDataVersion(@PathVariable("syncKey") String syncKey) {
        return syncDataLogsService.getDataVersion(syncKey);
    }

    /**
     * 获取同步日志的反馈状态
     *
     * @param dataVersion 数据版本
     * @return 结果
     */
    @PostMapping("/getSyncDataResult/{dataVersion}")
    public SyncDataLogs getSyncDataResult(@PathVariable("dataVersion") String dataVersion) {
        return syncDataLogsService.getSyncDataResult(dataVersion);
    }

    /**
     * 获取请求日志的反馈状态
     *
     * @param dataVersion 数据版本
     * @return 结果
     */
    @PostMapping("/getReqDataResult/{dataVersion}")
    public SyncDataLogs getReqDataResult(@PathVariable("dataVersion") String dataVersion) {
        return syncDataLogsService.getReqDataResult(dataVersion);
    }

    /**
     * 检查待发布排程记录是否已被锁定
     *
     * @param lockKey    锁key
     * @param publishIds 待发布记录ID
     * @return 结果
     */
    @PostMapping("/getReqDataResult/{lockKey}/{publishIds}")
    public boolean checkPublishLocking(@PathVariable("lockKey") String lockKey, @PathVariable("publishIds") Long[] publishIds) {
        return syncDataLogsService.checkPublishLocking(lockKey, publishIds);
    }
    
    /**
     * 获取版本号
     * @param syncKey
     * @return
     */
    @GetMapping(value = "/getDataVersion/{syncKey}")
    public AjaxResult getDataVersion(@PathVariable("syncKey") String syncKey) {
        logger.info("getDataVersion-001 获取 syncKey: " + syncKey + " 的版本号");
        SyncDataHandle handle = SpringUtils.getBean(SyncDataHandle.class);
        return AjaxResult.success(handle.getDataVersion(syncKey));
    }
}
