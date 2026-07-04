package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleRollingAdjustLog;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90RollingCheckRequest;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90ScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90ScheduleResultRemoteService {
    @ApiOperation("查询列表")
    @PostMapping("/cd90ScheduleResult/list")
    TableDataInfo list(@RequestBody Cd90ScheduleResult queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd90ScheduleResult/getInfo/{id}")
    Cd90ScheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("删除")
    @PostMapping("/cd90ScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("自动排程")
    @PostMapping("/cd90ScheduleResult/autoSchedule")
    AjaxResult autoSchedule(@RequestBody Cd90ScheduleResult scheduleResult);

    @ApiOperation("查询插单班次日期")
    @PostMapping("/cd90ScheduleResult/shiftDates")
    AjaxResult shiftDates(@RequestBody Cd90InsertOrderRequest request);

    @ApiOperation("插单预校验")
    @PostMapping("/cd90ScheduleResult/validateInsert")
    AjaxResult validateInsert(@RequestBody Cd90InsertOrderRequest request);

    @ApiOperation("提交插单滚动重排")
    @PostMapping("/cd90ScheduleResult/insert")
    AjaxResult insertOrder(@RequestBody Cd90InsertOrderRequest request);

    @ApiOperation("查询插单滚动重排任务")
    @GetMapping("/cd90ScheduleResult/insert/task/{taskId}")
    AjaxResult getInsertTask(@PathVariable("taskId") String taskId);
    @ApiOperation("检查定时滚动排程窗口")
    @PostMapping("/cd90ScheduleResult/rollingSchedule/check")
    AjaxResult checkTimedRolling(@RequestBody Cd90RollingCheckRequest request);

    @ApiOperation("查询定时滚动排程任务")
    @GetMapping("/cd90ScheduleResult/rollingSchedule/task/{taskId}")
    AjaxResult getTimedRollingTask(@PathVariable("taskId") String taskId);

    @ApiOperation("查询定时滚动调整日志列表")
    @PostMapping("/cd90ScheduleResult/rollingSchedule/adjustLog/list")
    TableDataInfo listRollingAdjustLogs(
            @RequestBody Cd90ScheduleRollingAdjustLog queryVO);

    @ApiOperation("查询定时滚动调整日志详情")
    @GetMapping("/cd90ScheduleResult/rollingSchedule/adjustLog/{id}")
    Cd90ScheduleRollingAdjustLog getRollingAdjustLog(
            @PathVariable("id") Long id);


    @ApiOperation("发布排程")
    @PostMapping("/cd90ScheduleResult/publish")
    AjaxResult publish(@RequestBody Cd90ScheduleResult dto,
                       @RequestParam(value = "ids", required = false) String ids);

    @ApiOperation("查询自动排程任务状态")
    @GetMapping("/cd90ScheduleResult/autoSchedule/task/{taskId}")
    AjaxResult getAutoScheduleTask(@PathVariable("taskId") String taskId);

    @ApiOperation("查询最近自动排程任务")
    @GetMapping("/cd90ScheduleResult/autoSchedule/task/latest")
    AjaxResult getLatestAutoScheduleTask(@RequestParam("factoryCode") String factoryCode,
                                         @RequestParam("scheduleDate") String scheduleDate);

    @ApiOperation("补偿自动排程超时任务")
    @PostMapping("/cd90ScheduleResult/autoSchedule/recoverTimeoutTasks")
    AjaxResult recoverAutoScheduleTimeoutTasks(
            @RequestParam(value = "timeoutMinutes", required = false) Integer timeoutMinutes);

    @ApiOperation("导出")
    @PostMapping("/cd90ScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90ScheduleResult queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/cd90ScheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
