package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.aps.cd15.api.domain.dto.Cd15ScheduleImportDTO;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 斜裁排程结果 Feign 接口。
 */
@FeignClient(contextId = "ICd15ScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15ScheduleResultRemoteService {

    @ApiOperation("查询斜裁排程结果列表")
    @PostMapping("/cd15ScheduleResult/list")
    TableDataInfo list(@RequestBody Cd15ScheduleResult queryVO);

    @ApiOperation("获取斜裁排程结果详情")
    @GetMapping("/cd15ScheduleResult/getInfo/{id}")
    Cd15ScheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("删除斜裁排程结果")
    @PostMapping("/cd15ScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("斜裁自动排程")
    @PostMapping("/cd15ScheduleResult/autoSchedule")
    AjaxResult autoSchedule(@RequestBody Cd15ScheduleResult scheduleResult);

    @ApiOperation("查询斜裁自动排程任务")
    @GetMapping("/cd15ScheduleResult/autoSchedule/task/{taskId}")
    AjaxResult getAutoScheduleTask(@PathVariable("taskId") String taskId);

    @ApiOperation("查询斜裁班次日期")
    @PostMapping("/cd15ScheduleResult/shiftDates")
    AjaxResult shiftDates(@RequestBody Cd15InsertOrderRequest request);

    @ApiOperation("斜裁插单预校验")
    @PostMapping("/cd15ScheduleResult/validateInsert")
    AjaxResult validateInsert(@RequestBody Cd15InsertOrderRequest request);

    @ApiOperation("提交斜裁插单")
    @PostMapping("/cd15ScheduleResult/insert")
    AjaxResult insert(@RequestBody Cd15InsertOrderRequest request);

    @ApiOperation("查询斜裁插单任务")
    @GetMapping("/cd15ScheduleResult/insert/task/{taskId}")
    AjaxResult getInsertTask(@PathVariable("taskId") String taskId);

    @ApiOperation("斜裁转机台预校验")
    @PostMapping("/cd15ScheduleResult/validateTransferMachine")
    AjaxResult validateTransferMachine(@RequestBody Cd15TransferMachineRequest request);

    @ApiOperation("提交斜裁转机台")
    @PostMapping("/cd15ScheduleResult/transferMachine")
    AjaxResult transferMachine(@RequestBody Cd15TransferMachineRequest request);

    @ApiOperation("查询斜裁转机台任务")
    @GetMapping("/cd15ScheduleResult/transferMachine/task/{taskId}")
    AjaxResult getTransferMachineTask(@PathVariable("taskId") String taskId);

    @ApiOperation("斜裁调量预校验")
    @PostMapping("/cd15ScheduleResult/validateChangeQty")
    AjaxResult validateChangeQty(@RequestBody Cd15ChangeQtyRequest request);

    @ApiOperation("提交斜裁调量")
    @PostMapping("/cd15ScheduleResult/changeQty")
    AjaxResult changeQty(@RequestBody Cd15ChangeQtyRequest request);

    @ApiOperation("查询斜裁调量任务")
    @GetMapping("/cd15ScheduleResult/changeQty/task/{taskId}")
    AjaxResult getChangeQtyTask(@PathVariable("taskId") String taskId);

    @ApiOperation("CD15定时滚动排程检查")
    @PostMapping("/cd15ScheduleResult/rollingSchedule/check")
    AjaxResult checkTimedRolling(@RequestBody Cd15RollingCheckRequest request);

    @ApiOperation("查询CD15定时滚动排程任务")
    @GetMapping("/cd15ScheduleResult/rollingSchedule/task/{taskId}")
    AjaxResult getTimedRollingTask(@PathVariable("taskId") String taskId);

    @ApiOperation("补偿斜裁自动排程超时任务")
    @PostMapping("/cd15ScheduleResult/autoSchedule/recoverTimeoutTasks")
    AjaxResult recoverAutoScheduleTimeoutTasks(
            @RequestParam(value = "timeoutMinutes", required = false)
            Integer timeoutMinutes);

    @ApiOperation("发布斜裁排程结果")
    @PostMapping("/cd15ScheduleResult/publish")
    AjaxResult publish(@RequestBody Cd15ScheduleResult dto,
                       @RequestParam(value = "ids", required = false) String ids);

    @ApiOperation("导出斜裁排程结果")
    @PostMapping("/cd15ScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15ScheduleResult queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入斜裁排程结果")
    @PostMapping("/cd15ScheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("按固定模板导入斜裁排程结果")
    @PostMapping("/cd15ScheduleResult/importDataByCust/{updateSupport}")
    AjaxResult importDataByCust(@PathVariable("updateSupport") boolean updateSupport,
                                @RequestBody Cd15ScheduleImportDTO importDTO);
}
