package com.ruoyi.job.controller;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.job.api.domain.SysJob;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.job.service.ISysJobService;
import com.ruoyi.job.util.CronUtils;

/**
 * 调度任务信息操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/job")
public class SysJobController extends BaseController
{
    @Autowired
    private ISysJobService jobService;

    /**
     * 查询定时任务列表
     */
    @PreAuthorize(hasPermi = "monitor:job:list")
    @GetMapping("/list")
    public TableDataInfo list(SysJob sysJob)
    {
        startPage();
        List<SysJob> list = jobService.selectJobList(sysJob);
        return getDataTable(list);
    }

    /**
     * 导出定时任务列表
     */
    @PreAuthorize(hasPermi = "monitor:job:export")
    @Log(title = "job.title.timejob", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysJob sysJob) throws IOException
    {
        List<SysJob> list = jobService.selectJobList(sysJob);
        ExcelUtil<SysJob> util = new ExcelUtil<SysJob>(SysJob.class);
        util.exportExcel(response, list, I18nUtil.getMessage("job.title.timejob"));
    }

    /**
     * 获取定时任务详细信息
     */
    @PreAuthorize(hasPermi = "monitor:job:query")
    @GetMapping(value = "/{jobId}")
    public AjaxResult getInfo(@PathVariable("jobId") Long jobId)
    {
        return AjaxResult.success(jobService.selectJobById(jobId));
    }

    /**
     * 新增定时任务
     */
    @PreAuthorize(hasPermi = "monitor:job:add")
    @Log(title = "job.title.timejob", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysJob sysJob) throws SchedulerException, TaskException
    {
        if (!CronUtils.isValid(sysJob.getCronExpression()))
        {
            return AjaxResult.error(I18nUtil.getMessage("job.error.cron.wrong"));
        }
        sysJob.setCreateBy(SecurityUtils.getUsername());
        return toAjax(jobService.insertJob(sysJob));
    }

    /**
     * 修改定时任务
     */
    @PreAuthorize(hasPermi = "monitor:job:edit")
    @Log(title = "job.title.timejob", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysJob sysJob) throws SchedulerException, TaskException
    {
        if (!CronUtils.isValid(sysJob.getCronExpression()))
        {
            return AjaxResult.error(I18nUtil.getMessage("job.error.cron.wrong"));
        }
        sysJob.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(jobService.updateJob(sysJob));
    }

    /**
     * 定时任务状态修改
     */
    @PreAuthorize(hasPermi = "monitor:job:changeStatus")
    @Log(title = "job.title.timejob", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysJob job) throws SchedulerException
    {
        SysJob newJob = jobService.selectJobById(job.getJobId());
        newJob.setStatus(job.getStatus());
        return toAjax(jobService.changeStatus(newJob));
    }

    /**
     * 定时任务立即执行一次
     */
    @PreAuthorize(hasPermi = "monitor:job:changeStatus")
    @Log(title = "job.title.timejob", businessType = BusinessType.UPDATE)
    @PutMapping("/run")
    public AjaxResult run(@RequestBody SysJob job) throws SchedulerException
    {
        jobService.run(job);
        return AjaxResult.success();
    }

    /**
     * 删除定时任务
     */
    @PreAuthorize(hasPermi = "monitor:job:remove")
    @Log(title = "job.title.timejob", businessType = BusinessType.DELETE)
    @DeleteMapping("/{jobIds}")
    public AjaxResult remove(@PathVariable Long[] jobIds) throws SchedulerException, TaskException
    {
        jobService.deleteJobByIds(jobIds);
        return AjaxResult.success();
    }

    /**
     * 根据任务ID获取对应的任务
     * @param jobId
     * @return
     */
    @PostMapping("/selectJobById")
    public SysJob selectJobById(Long jobId){
        return jobService.selectJobById(jobId);
    }

    /**
     * 校验cron表达式是否有效
     * @param job
     * @return
     */
    @PostMapping("/checkCronExpressionIsValid")
    public boolean checkCronExpressionIsValid(SysJob job){
        return jobService.checkCronExpressionIsValid(job.getCronExpression());
    }

    /**
     * 校验cron表达式是否有效
     * @param job
     * @return
     */
    @PostMapping("/checkCronExpressionIsValidInUI")
    boolean checkCronExpressionIsValidInUI(@RequestBody SysJob job){
        return checkCronExpressionIsValid(job);
    }
}
