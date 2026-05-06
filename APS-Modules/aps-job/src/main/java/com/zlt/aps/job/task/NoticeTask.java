package com.zlt.aps.job.task;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.job.service.INoticeService;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.service.IMdmWorkCalendarRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 发送消息通知
 *
 * @author zlt
 */
@Component("noticeTask")
@Slf4j
public class NoticeTask {
    @Autowired(required = false)
    private INoticeService iNoticeService;

    /**
     * 检查aps-mps服务是否可用
     * aps-mps服务未部署时，Feign客户端不会注入，返回false
     * @return true-可用 false-不可用
     */
    private boolean isMpsAvailable() {
        if (iNoticeService == null) {
            log.warn("aps-mps服务未启动，跳过消息通知");
            return false;
        }
        return true;
    }

    /**
     * 完成生产结果通知
     */
    public void unfinishedSchedule() {
        if (!isMpsAvailable()) return;
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

    /**
     * 定时生成下一年工作日历(所有工序)
     */
    public void genAnnualPlan() {
        MdmWorkCalendar entity = new MdmWorkCalendar();
        entity.setYear(DateUtils.getYear(new Date()) + 1);
        AjaxResult ajaxResult = iMdmWorkCalendarRemoteService.genAnnualPlan(entity);
        log.info("接口返回：{}", JSON.toJSONString(ajaxResult));
    }
}
