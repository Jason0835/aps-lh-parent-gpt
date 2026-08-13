package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;
import com.zlt.aps.cd15.api.service.ICd15ScheduleResultRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 固定在斜裁交班前45、50、55分调用滚动检查。
 */
@Slf4j
@Component("cd15RollingScheduleTask")
@RequiredArgsConstructor
public class Cd15RollingScheduleTask {

    private final ICd15ScheduleResultRemoteService remoteService;

    /** 平台任务调用入口，无业务参数。 */
    public void execute() {
        FeignTokenHelper.runWithToken(() -> {
            Cd15RollingCheckRequest request = new Cd15RollingCheckRequest();
            request.setTriggerTime(new Date());
            AjaxResult result = this.remoteService.checkTimedRolling(request);
            log.info("[斜裁定时滚动] Job检查完成, result={}", result);
        });
    }
}
