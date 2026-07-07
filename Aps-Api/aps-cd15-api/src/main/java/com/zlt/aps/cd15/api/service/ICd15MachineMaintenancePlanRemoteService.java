package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd15MachineMaintenancePlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15MachineMaintenancePlanRemoteService {

    @ApiOperation("查询斜裁机台检修计划列表")
    @PostMapping("/cd15MachineMaintenance/list")
    TableDataInfo list(@RequestBody Cd15MachineMaintenancePlan queryVO);

    @ApiOperation("获取斜裁机台检修计划详情")
    @GetMapping("/cd15MachineMaintenance/getInfo/{id}")
    Cd15MachineMaintenancePlan getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增斜裁机台检修计划")
    @PostMapping("/cd15MachineMaintenance/add")
    AjaxResult add(@RequestBody Cd15MachineMaintenancePlan entity);

    @ApiOperation("编辑斜裁机台检修计划")
    @PostMapping("/cd15MachineMaintenance/edit")
    AjaxResult edit(@RequestBody Cd15MachineMaintenancePlan entity);

    @ApiOperation("删除斜裁机台检修计划")
    @PostMapping("/cd15MachineMaintenance/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验斜裁机台检修计划唯一性")
    @PostMapping("/cd15MachineMaintenance/checkUnique")
    String checkUnique(@RequestBody Cd15MachineMaintenancePlan entity);

    @ApiOperation("校验斜裁机台检修计划时间段重叠")
    @PostMapping("/cd15MachineMaintenance/checkOverlap")
    String checkOverlap(@RequestBody Cd15MachineMaintenancePlan entity);

    @ApiOperation("导出斜裁机台检修计划")
    @PostMapping("/cd15MachineMaintenance/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15MachineMaintenancePlan queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入斜裁机台检修计划")
    @PostMapping("/cd15MachineMaintenance/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}