package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.tc.api.service.ITcScheduleResultRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 胎侧发布超时任务恢复平台任务。
 */
@Slf4j
@Component("tcReleaseRecoveryTask")
public class TcReleaseRecoveryTask {

    @Resource
    private ITcScheduleResultRemoteService remoteService;

    /**
     * 平台任务调用入口，无业务参数。
     */
    public void execute() {
        FeignTokenHelper.runWithToken(() -> {
            AjaxResult result = this.remoteService.recoverReleaseTimeout();
            log.info("[胎侧发布恢复] Job检查完成, result={}", result);
        });
    }
}
