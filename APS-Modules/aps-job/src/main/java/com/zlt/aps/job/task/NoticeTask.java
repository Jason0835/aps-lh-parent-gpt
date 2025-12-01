package com.zlt.aps.job.task;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.zlt.aps.job.service.INoticeService;

/**
 * 发送消息通知
 * 
 * @author zlt
 */
@Component("noticeTask")
public class NoticeTask {
    @Resource
    private INoticeService iNoticeService;

    /**
     * 完成生产结果通知
     */
    public void unfinishedSchedule() {
        iNoticeService.unfinishedSchedule();
    }
}
