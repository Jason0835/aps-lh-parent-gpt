package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineMaintenancePlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICd90MachineMaintenancePlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90MachineMaintenancePlanRemoteService {

    @ApiOperation("查询直裁机台检修计划列表")
    @PostMapping("/cd90MachineMaintenance/list")
    TableDataInfo list(@RequestBody Cd90MachineMaintenancePlan queryVO);

    @ApiOperation("获取直裁机台检修计划详情")
    @GetMapping("/cd90MachineMaintenance/getInfo/{id}")
    Cd90MachineMaintenancePlan getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增直裁机台检修计划")
    @PostMapping("/cd90MachineMaintenance/add")
    AjaxResult add(@RequestBody Cd90MachineMaintenancePlan entity);

    @ApiOperation("编辑直裁机台检修计划")
    @PostMapping("/cd90MachineMaintenance/edit")
    AjaxResult edit(@RequestBody Cd90MachineMaintenancePlan entity);

    @ApiOperation("删除直裁机台检修计划")
    @PostMapping("/cd90MachineMaintenance/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验直裁机台检修计划唯一性")
    @PostMapping("/cd90MachineMaintenance/checkUnique")
    String checkUnique(@RequestBody Cd90MachineMaintenancePlan entity);

    @ApiOperation("导出直裁机台检修计划")
    @PostMapping("/cd90MachineMaintenance/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90MachineMaintenancePlan queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入直裁机台检修计划")
    @PostMapping("/cd90MachineMaintenance/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}