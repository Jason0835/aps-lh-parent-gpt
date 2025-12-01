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

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITmMachineMaintenanceRemoteService.java
 * 描    述：ITmMachineMaintenanceRemoteService胎面机台维修计划前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-15
 */
@FeignClient(contextId = "ITmMachineMaintenanceRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmMachineMaintenanceRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/tmMachineMaintenance/list")
    TableDataInfo list(@RequestBody TmMachineMaintenance QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/tmMachineMaintenance/save")
    AjaxResult save(@RequestBody TmMachineMaintenance tmMachineMaintenance);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/tmMachineMaintenance/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmMachineMaintenance/{id}")
    TmMachineMaintenance getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/tmMachineMaintenance/checkUnique")
    String checkUnique(@RequestBody TmMachineMaintenance tmMachineMaintenanceVO);

    /**
     * 导出胎面机台维修计划列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/tmMachineMaintenance/exportData/{fileName}")
    byte[] exportData(@RequestBody TmMachineMaintenance queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入胎面机台维修计划数据
     */
    @ApiOperation("导入胎面机台维修计划")
    @PostMapping("/tmMachineMaintenance/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
