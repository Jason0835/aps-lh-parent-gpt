package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.tm.api.domain.dto.TmRollingCheckRequestDTO;
import com.zlt.aps.tm.api.service.ITmScheduleResultRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 每分钟检查胎面自动滚动班次窗口。
 */
@Slf4j
@Component("tmRollingScheduleTask")
public class TmRollingScheduleTask {

    @Resource
    private ITmScheduleResultRemoteService remoteService;

    /**
     * 平台任务调用入口，无业务参数。
     */
    public void execute() {
        FeignTokenHelper.runWithToken(() -> {
            TmRollingCheckRequestDTO request = new TmRollingCheckRequestDTO();
            request.setTriggerTime(new Date());
            AjaxResult result = this.remoteService.checkTimedRolling(request);
            log.info("[胎面定时滚动] Job检查完成，result={}", result);
        });
    }
}
