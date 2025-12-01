package com.zlt.aps.job.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 消息通知接口
 * 
 * @author zlt
 */
@FeignClient(contextId = "INoticeService", value = "aps-mps")
public interface INoticeService {
    
    /**
     * 未完成生产结果通知
     * 
     */
    @PostMapping(value = "/messageNotice/unfinishedSchedule")
    AjaxResult unfinishedSchedule();
}
