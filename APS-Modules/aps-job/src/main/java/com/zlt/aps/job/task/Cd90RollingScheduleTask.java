package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cd90.api.domain.vo.Cd90RollingCheckRequest;
import com.zlt.aps.cd90.api.service.ICd90ScheduleResultRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/** 每5分钟调用一次CD90交班滚动窗口检查。 */
@Slf4j
@Component("cd90RollingScheduleTask")
public class Cd90RollingScheduleTask {

    @Resource
    private ICd90ScheduleResultRemoteService remoteService;

    /** 平台任务调用入口，无业务参数。 */
    public void execute() {
        FeignTokenHelper.runWithToken(() -> {
            Cd90RollingCheckRequest request = new Cd90RollingCheckRequest();
            request.setTriggerTime(new Date());
            AjaxResult result = remoteService.checkTimedRolling(request);
            log.info("[直裁定时滚动] Job检查完成, result={}", result);
        });
    }
}
