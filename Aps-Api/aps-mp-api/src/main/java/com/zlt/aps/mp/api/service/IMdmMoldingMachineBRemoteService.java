package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachineB;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMoldingMachineBRemoteService.java
 * 描    述：IMdmMoldingMachineBRemoteService基础数据-成型机子前端接口
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmMoldingMachineBRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.maindata:/maindata}")
public interface IMdmMoldingMachineBRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmMoldingMachineB/list")
    TableDataInfo list(@RequestBody MdmMoldingMachineB QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmMoldingMachineB/save")
    AjaxResult save(@RequestBody MdmMoldingMachineB mdmMoldingMachineB);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmMoldingMachineB/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMoldingMachineB/{id}")
    MdmMoldingMachineB getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmMoldingMachineB/checkUnique")
    String checkUnique(@RequestBody MdmMoldingMachineB mdmMoldingMachineBVO);

    /**
     * 导出基础数据-成型机子列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmMoldingMachineB/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMoldingMachineB queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入基础数据-成型机子数据
     */
    @ApiOperation("导入基础数据-成型机子")
    @PostMapping("/mdmMoldingMachineB/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
