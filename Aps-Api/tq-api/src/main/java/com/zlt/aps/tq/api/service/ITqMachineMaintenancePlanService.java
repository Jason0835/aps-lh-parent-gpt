package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineMaintenancePlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITqMachineMaintenancePlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqMachineMaintenancePlanService {

    @PostMapping("/tqMachineMaintenancePlan/list")
    @ApiOperation("查询胎圈机台维修计划列表")
    TableDataInfo list(@RequestBody TqMachineMaintenancePlan entity);

    @GetMapping(value = "/tqMachineMaintenancePlan/{id}")
    @ApiOperation("获取胎圈机台维修计划详细信息")
    TqMachineMaintenancePlan getInfo(@PathVariable("id") Long id);

    @PostMapping("/tqMachineMaintenancePlan/save")
    @ApiOperation("保存胎圈机台维修计划（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody TqMachineMaintenancePlan entity);

    @PostMapping("/tqMachineMaintenancePlan/delete/{ids}")
    @ApiOperation("删除胎圈机台维修计划")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    @PostMapping("/tqMachineMaintenancePlan/exportData/{fileName}")
    @ApiOperation("导出胎圈机台维修计划")
    byte[] exportData(@RequestBody TqMachineMaintenancePlan entity, @PathVariable("fileName") String fileName);

    @PostMapping("/tqMachineMaintenancePlan/exportList")
    @ApiOperation("导出胎圈机台维修计划列表")
    List<TqMachineMaintenancePlan> exportList(@RequestBody TqMachineMaintenancePlan entity);

    @PostMapping("/tqMachineMaintenancePlan/importData")
    @ApiOperation("导入胎圈机台维修计划")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @PostMapping("/tqMachineMaintenancePlan/deleteAll")
    @ApiOperation("删除全部(逻辑删)")
    AjaxResult deleteAll();
}
