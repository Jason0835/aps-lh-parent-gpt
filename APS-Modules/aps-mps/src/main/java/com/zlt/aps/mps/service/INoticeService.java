package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * 消息通知接口
 * @author zlt
 *
 */
public interface INoticeService {
    /**
     * 未完成生产结果通知
     * @return
     */
    AjaxResult unfinishedSchedule();
}
