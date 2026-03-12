package com.zlt.aps.job.task;

import com.zlt.aps.job.service.INoticeService;
import com.zlt.aps.mp.api.service.IMdmWorkCalendarRemoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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

    @Autowired
    private IMdmWorkCalendarRemoteService iMdmWorkCalendarRemoteService;

    /**
     * 定时通知计划员维护日历
     */
    public void workCalendarNotice() {
        iMdmWorkCalendarRemoteService.workCalendarNotice();
    }
}
