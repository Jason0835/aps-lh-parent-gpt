package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineMaintenance;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITcMachineMaintenanceRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcMachineMaintenanceRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcMachineMaintenance/list")
    TableDataInfo list(@RequestBody TcMachineMaintenance queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcMachineMaintenance/save")
    AjaxResult save(TcMachineMaintenance tcMachineMaintenance);

    @ApiOperation("删除")
    @DeleteMapping("/tcMachineMaintenance/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcMachineMaintenance/{id}")
    TcMachineMaintenance getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcMachineMaintenance/checkUnique")
    String checkUnique(@RequestBody TcMachineMaintenance tcMachineMaintenanceVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcMachineMaintenance/exportData/{fileName}")
    byte[] exportData(@RequestBody TcMachineMaintenance queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcMachineMaintenance/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}