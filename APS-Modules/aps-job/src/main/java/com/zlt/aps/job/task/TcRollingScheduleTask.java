package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.tc.api.domain.vo.TcRollingCheckRequestVo;
import com.zlt.aps.tc.api.service.ITcScheduleResultRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 每五分钟检查一次胎侧自动滚动班次窗口。
 */
@Slf4j
@Component("tcRollingScheduleTask")
public class TcRollingScheduleTask {

    @Resource
    private ITcScheduleResultRemoteService remoteService;

    /**
     * 平台任务调用入口，无业务参数。
     */
    public void execute() {
        FeignTokenHelper.runWithToken(() -> {
            TcRollingCheckRequestVo request = new TcRollingCheckRequestVo();
            request.setTriggerTime(new Date());
            AjaxResult result = this.remoteService.checkTimedRolling(request);
            log.info("[胎侧定时滚动] Job检查完成, result={}", result);
        });
    }
}
