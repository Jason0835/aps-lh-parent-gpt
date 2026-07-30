package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingCheckRequestVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 钢丝圈排程后台任务远程服务。
 *
 * <p>供 aps-job 模块通过 Gateway + Feign 调用钢丝圈微服务的内部接口，
 * 与 {@code ITcScheduleResultRemoteService} 对齐。</p>
 *
 * @author APS
 */
@FeignClient(contextId = "IGsqScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.gsq:gsq}")
public interface IGsqScheduleResultRemoteService {

    /**
     * 检查并提交钢丝圈自动滚动任务。
     *
     * @param request 检查请求
     * @return 检查结果
     */
    @ApiOperation("检查钢丝圈自动滚动窗口")
    @PostMapping("/scheduleResult/internal/checkTimedRolling")
    AjaxResult checkTimedRolling(@RequestBody GsqRollingCheckRequestVo request);
}
