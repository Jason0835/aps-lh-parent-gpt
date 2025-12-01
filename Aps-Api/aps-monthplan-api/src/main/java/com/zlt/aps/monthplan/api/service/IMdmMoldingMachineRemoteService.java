package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMoldingMachineRemoteService.java
 * 描    述：IMdmMoldingMachineRemoteService基础数据-成型机档案前端接口
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmMoldingMachineRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMoldingMachineRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmMoldingMachine/list")
    TableDataInfo list(@RequestBody MdmMoldingMachine QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmMoldingMachine/save")
    AjaxResult save(@RequestBody MdmMoldingMachine mdmMoldingMachine);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmMoldingMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMoldingMachine/{id}")
    MdmMoldingMachine getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmMoldingMachine/checkUnique")
    String checkUnique(@RequestBody MdmMoldingMachine mdmMoldingMachineVO);

    /**
     * 导出基础数据-成型机档案列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmMoldingMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMoldingMachine queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入基础数据-成型机档案数据
     */
    @ApiOperation("导入基础数据-成型机档案")
    @PostMapping("/mdmMoldingMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
