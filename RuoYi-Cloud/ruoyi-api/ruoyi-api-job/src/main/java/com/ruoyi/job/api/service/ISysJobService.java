package com.ruoyi.job.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.job.api.domain.SysJob;
import org.quartz.SchedulerException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 定义调用定时作业模块相关接口服务
 */
@FeignClient(contextId = "iSysJobService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.job:schedule/job}")
public interface ISysJobService {

    /**
     * 查询定时任务列表
     * @param sysJob
     * @return
     */
    @GetMapping("/list")
    TableDataInfo list(@SpringQueryMap SysJob sysJob);

    /**
     * 导出定时任务列表
     * @param response
     * @param sysJob
     * @throws IOException
     */
    /*@PostMapping("job/job/export")
    void export(HttpServletResponse response, @RequestBody SysJob sysJob) throws IOException;*/

    /**
     * 获取定时任务详细信息
     * @param jobId
     * @return
     */
    @GetMapping(value = "/{jobId}")
    AjaxResult getInfo(@PathVariable(value="jobId") Long jobId);

    /**
     * 新增定时任务
     * @param sysJob
     * @return
     * @throws SchedulerException
     * @throws TaskException
     */
    @PostMapping(value = "")
    AjaxResult add(@RequestBody SysJob sysJob) throws SchedulerException, TaskException;

    /**
     * 修改定时任务
     * @param sysJob
     * @return
     * @throws SchedulerException
     * @throws TaskException
     */
    @PutMapping(value = "")
    AjaxResult edit(@RequestBody SysJob sysJob) throws SchedulerException, TaskException;

    /**
     * 定时任务状态修改
     * @param job
     * @return
     * @throws SchedulerException
     */
    @PutMapping("/changeStatus")
    AjaxResult changeStatus(@RequestBody SysJob job) throws SchedulerException;

    /**
     * 定时任务立即执行一次
     * @param job
     * @return
     * @throws SchedulerException
     */
    @PutMapping("/run")
    AjaxResult run(@RequestBody SysJob job) throws SchedulerException;

    /**
     * 删除定时任务
     * @param jobIds
     * @return
     * @throws SchedulerException
     * @throws TaskException
     */
    @DeleteMapping("/{jobIds}")
    AjaxResult remove(@PathVariable(value="jobIds") Long[] jobIds) throws SchedulerException, TaskException;

    /**
     * 根据任务ID获取任务信息
     * @param jobId
     * @return
     */
    @PostMapping("/selectJobById")
    SysJob selectJobById(@RequestParam("jobId") Long jobId);

    /**
     * 校验cron表达式是否有效,仅给vue使用
     * @param job
     * @return
     */
    @PostMapping("/checkCronExpressionIsValid")
    boolean checkCronExpressionIsValid(SysJob job);

    /**
     * 校验cron表达式是否有效，给bootui使用
     * @param job
     * @return
     */
    @GetMapping("/checkCronExpressionIsValidInUI")
    boolean checkCronExpressionIsValidInUI(@RequestBody SysJob job);
}
