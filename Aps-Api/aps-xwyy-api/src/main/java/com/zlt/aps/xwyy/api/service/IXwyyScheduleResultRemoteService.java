package com.zlt.aps.xwyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "IXwyyScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyScheduleResultRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/xwyyScheduleResult/list")
    TableDataInfo list(@RequestBody XwyyScheduleResult queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/xwyyScheduleResult/getInfo/{id}")
    XwyyScheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("删除")
    @PostMapping("/xwyyScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("自动排程")
    @PostMapping("/xwyyScheduleResult/autoSchedule")
    AjaxResult autoSchedule(@RequestBody XwyyScheduleResult entity);

    @ApiOperation("自动排程任务查询")
    @GetMapping("/xwyyScheduleResult/autoSchedule/task/{taskId}")
    AjaxResult getAutoScheduleTask(@PathVariable("taskId") String taskId);

    @ApiOperation("插单")
    @PostMapping("/xwyyScheduleResult/insert")
    AjaxResult insert(@RequestBody XwyyScheduleResult entity);

    @ApiOperation("转机台")
    @PostMapping("/xwyyScheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody XwyyScheduleResult entity);

    @ApiOperation("调量")
    @PostMapping("/xwyyScheduleResult/adjustQty")
    AjaxResult adjustQty(@RequestBody XwyyScheduleResult entity);

    @ApiOperation("发布")
    @PostMapping("/xwyyScheduleResult/publish")
    AjaxResult publish(@RequestBody XwyyScheduleResult entity);

    @ApiOperation("导出")
    @PostMapping("/xwyyScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody XwyyScheduleResult queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入")
    @PostMapping("/xwyyScheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
