package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingCheckRequestVo;
import com.zlt.aps.gsq.api.service.IGsqScheduleResultRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 钢丝圈自动滚动班次窗口检查定时任务。
 *
 * <p>平台任务调用入口，无业务参数。
 * 对齐 {@link TcRollingScheduleTask} 的 Job 入口模式，
 * 通过 Feign + Gateway 调用钢丝圈微服务内部接口。</p>
 *
 * @author APS
 */
@Slf4j
@Component("gsqRollingScheduleTask")
public class GsqRollingScheduleTask {

    @Resource
    private IGsqScheduleResultRemoteService remoteService;

    /**
     * 平台任务调用入口，无业务参数。
     */
    public void execute() {
        FeignTokenHelper.runWithToken(() -> {
            GsqRollingCheckRequestVo request = new GsqRollingCheckRequestVo();
            request.setTriggerTime(new Date());
            AjaxResult result = this.remoteService.checkTimedRolling(request);
            log.info("[钢丝圈定时滚动] Job检查完成, result={}", result);
        });
    }
}
