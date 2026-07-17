package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.vo.TcRollingCheckRequestVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 胎侧排程后台任务远程服务。
 */
@FeignClient(contextId = "ITcScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.tc:/tc}")
public interface ITcScheduleResultRemoteService {

    /**
     * 检查并提交胎侧自动滚动任务。
     *
     * @param request 检查请求
     * @return 检查结果
     */
    @ApiOperation("检查胎侧自动滚动窗口")
    @PostMapping("/tcScheduleResult/internal/checkTimedRolling")
    AjaxResult checkTimedRolling(@RequestBody TcRollingCheckRequestVo request);

    /**
     * 恢复发布超时任务。
     *
     * @return 恢复结果
     */
    @ApiOperation("恢复胎侧发布超时任务")
    @PostMapping("/tcScheduleResult/internal/recoverReleaseTimeout")
    AjaxResult recoverReleaseTimeout();
}
