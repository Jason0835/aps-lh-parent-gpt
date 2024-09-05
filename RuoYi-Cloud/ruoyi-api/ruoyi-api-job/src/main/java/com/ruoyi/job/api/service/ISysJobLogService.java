package com.ruoyi.job.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.job.api.domain.SysJobLog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 调度日志外放接口
 */
@FeignClient(contextId = "iSysJobLogService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.job:schedule/job}")
public interface ISysJobLogService {

    /**
     * 查询定时任务调度日志列表
     * @param sysJobLog
     * @return
     */
    @GetMapping("/log/list")
    TableDataInfo list(@SpringQueryMap SysJobLog sysJobLog);

    /**
     * 导出定时任务调度日志列表
     * @param response
     * @param sysJobLog
     * @throws IOException
     */
   /* @PostMapping("job/job/log/export")
    void export(HttpServletResponse response, SysJobLog sysJobLog) throws IOException;*/

    /**
     * 根据调度编号获取详细信息
     * @param jobLogId
     * @return
     */
    @GetMapping(value = "/log/{configId}")
    AjaxResult getInfo(@PathVariable("configId") Long jobLogId);

    /**
     * 删除定时任务调度日志
     * @param jobLogIds
     * @return
     */
    @DeleteMapping("/log/{jobLogIds}")
    public AjaxResult remove(@PathVariable("jobLogIds") Long[] jobLogIds);

    /**
     * 清空定时任务调度日志
     * @return
     */
    @DeleteMapping("/log/clean")
    public AjaxResult clean();

    /**
     * 根据任务ID获取任务信息
     * @param jobId
     * @return
     */
    @PostMapping("/log/selectJobLogById")
    SysJobLog selectJobLogById(@RequestParam("jobId") Long jobId);
}
