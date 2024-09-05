package com.zlt.kettle.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.common.utils.WebClientUtils;
import com.zlt.kettle.api.domain.JobRecord;
import com.zlt.kettle.api.domain.TaskInfo;
import com.zlt.kettle.api.domain.TransRecord;
import com.zlt.kettle.service.KettleBizLogServcie;
import com.zlt.kettle.service.KettleTaskInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.kettle.scheduler.common.povo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api("Kettle数据接口")
@RestController
@RequestMapping("/kettle")
public class KettleBizController extends BaseController {

    @Autowired
    KettleBizLogServcie kettleBizLogServcie;

    @Autowired
    KettleTaskInfoService kettleTaskInfoService;

    @Autowired
    WebClientUtils webClientUtils;

    @PostMapping("/transLog")
    @ApiOperation("转换日志")
    public TableDataInfo getTransLogs( TransRecord transRecord) {
        startPage();
        List<TransRecord> result = kettleBizLogServcie.getTransRecordList(transRecord);
        return getDataTable(result);
    }

    @PostMapping("/jobLog")
    @ApiOperation("Job日志")
    public TableDataInfo getJobLogs( JobRecord jobRecord) {
        startPage();
        List<JobRecord> result = kettleBizLogServcie.getJobRecordList(jobRecord);
        return getDataTable(result);
    }

    @PostMapping("/taskInfo")
    @ApiOperation("任务信息")
    public TableDataInfo getTaskInfo( TaskInfo taskInfo) {
        startPage();
        List<TaskInfo> result = kettleTaskInfoService.getTaskInfoList(taskInfo);
        return getDataTable(result);
    }

    @GetMapping("/start/{taskType}/{id}")
    @ApiOperation("执行一个kettle任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskType", value = "任务类型", required = true, dataType = "String"),
            @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Long")
    })
    public AjaxResult startTrans(
            @PathVariable("taskType") String taskType,
            @PathVariable("id") Integer id) {

        Result output = kettleTaskInfoService.runOneTaskInfo(id, taskType);
        AjaxResult ajaxResult = null;
        if (output.isSuccess()) {
            ajaxResult = AjaxResult.success(output.getMessage());
        } else {
            ajaxResult = AjaxResult.error(output.getCode() + ":" + output.getMessage(), output.getResult());
        }

        return ajaxResult;
    }
}
