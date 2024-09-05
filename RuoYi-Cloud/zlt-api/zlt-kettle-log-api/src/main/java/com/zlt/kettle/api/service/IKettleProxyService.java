package com.zlt.kettle.api.service;


import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.kettle.api.domain.JobRecord;
import com.zlt.kettle.api.domain.TaskInfo;
import com.zlt.kettle.api.domain.TransRecord;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "iKettleProxyService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.kettle:kettle}")
public interface IKettleProxyService {

    @PostMapping("/kettle/transLog")
    @ApiOperation("转换日志")
    public TableDataInfo getTransLogs(@RequestBody TransRecord transRecord);

    @PostMapping("/kettle/jobLog")
    @ApiOperation("Job日志")
    public TableDataInfo getJobLogs(@RequestBody JobRecord jobRecord);

    @PostMapping("/kettle/taskInfo")
    @ApiOperation("任务信息")
    public TableDataInfo getTaskInfo(@RequestBody TaskInfo taskInfo);

    @GetMapping("/kettle/start/{taskType}/{id}")
    public AjaxResult startTrans(
            @PathVariable("taskType") String taskType,
            @PathVariable("id") Integer id);
}
