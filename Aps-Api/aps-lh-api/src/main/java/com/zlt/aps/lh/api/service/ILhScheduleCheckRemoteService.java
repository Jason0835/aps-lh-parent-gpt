package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleCheckReportVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 硫化排程结果合理性校验远程接口。
 *
 * <p>校验键为「工厂编号 + 业务排程日」，排程批次号仅作追溯字段。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@FeignClient(contextId = "ILhScheduleCheckService", value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.lh:/lh}")
public interface ILhScheduleCheckRemoteService {

    /**
     * 查询校验报告
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return 校验报告
     */
    @ApiOperation("查询硫化排程校验报告")
    @GetMapping("/scheduleCheck/result")
    LhScheduleCheckReportVO getReport(@RequestParam("factoryCode") String factoryCode,
                                      @RequestParam("scheduleDate") String scheduleDate);

    /**
     * 查询是否存在未确认异常
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return true-存在未确认异常
     */
    @ApiOperation("查询是否存在未确认校验异常")
    @GetMapping("/scheduleCheck/pending")
    Boolean hasPending(@RequestParam("factoryCode") String factoryCode,
                       @RequestParam("scheduleDate") String scheduleDate);

    /**
     * 将某工厂某业务日的所有异常整体标记为已了解
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return 操作结果
     */
    @ApiOperation("确认硫化排程校验异常")
    @PostMapping("/scheduleCheck/acknowledge")
    AjaxResult acknowledge(@RequestParam("factoryCode") String factoryCode,
                           @RequestParam("scheduleDate") String scheduleDate);

    /**
     * 手动重新执行校验
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return 重新校验后的报告
     */
    @ApiOperation("重新执行硫化排程校验")
    @PostMapping("/scheduleCheck/rerun")
    LhScheduleCheckReportVO rerun(@RequestParam("factoryCode") String factoryCode,
                                  @RequestParam("scheduleDate") String scheduleDate);
}
