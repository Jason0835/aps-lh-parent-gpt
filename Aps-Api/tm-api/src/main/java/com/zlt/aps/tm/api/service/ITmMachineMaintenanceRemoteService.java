package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ITmMachineMaintenanceRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmMachineMaintenanceRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmMachineMaintenance/list")
    TableDataInfo list(@RequestBody TmMachineMaintenance queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmMachineMaintenance/save")
    AjaxResult save(TmMachineMaintenance tmMachineMaintenance);

    @ApiOperation("删除")
    @DeleteMapping("/tmMachineMaintenance/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmMachineMaintenance/{id}")
    TmMachineMaintenance getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmMachineMaintenance/checkUnique")
    String checkUnique(@RequestBody TmMachineMaintenance tmMachineMaintenanceVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmMachineMaintenance/exportData/{fileName}")
    byte[] exportData(@RequestBody TmMachineMaintenance queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmMachineMaintenance/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
