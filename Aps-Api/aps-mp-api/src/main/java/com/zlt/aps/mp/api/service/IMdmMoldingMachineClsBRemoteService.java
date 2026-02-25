package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachineClsB;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMoldingMachineClsBRemoteService.java
 * 描    述：IMdmMoldingMachineClsBRemoteService成型机单机班产前端接口
 *@author zlt
 *@date 2025-02-27
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmMoldingMachineClsBRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMoldingMachineClsBRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmMoldingMachineClsB/list")
    TableDataInfo list(@RequestBody MdmMoldingMachineClsB QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmMoldingMachineClsB/save")
    AjaxResult save(@RequestBody MdmMoldingMachineClsB mdmMoldingMachineClsB);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmMoldingMachineClsB/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMoldingMachineClsB/{id}")
    MdmMoldingMachineClsB getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmMoldingMachineClsB/checkUnique")
    String checkUnique(@RequestBody MdmMoldingMachineClsB mdmMoldingMachineClsBVO);

    /**
     * 导出成型机单机班产列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmMoldingMachineClsB/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMoldingMachineClsB queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型机单机班产数据
     */
    @ApiOperation("导入成型机单机班产")
    @PostMapping("/mdmMoldingMachineClsB/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
