package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhSpecifyMachineRemoteService.java
 * 描    述：ILhSpecifyMachineRemoteService硫化定点机台信息前端接口
 *@author zlt
 *@date 2025-03-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ILhSpecifyMachineRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhSpecifyMachineRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhSpecifyMachine/list")
    TableDataInfo list(@RequestBody LhSpecifyMachine QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/lhSpecifyMachine/save")
    AjaxResult save(@RequestBody LhSpecifyMachine lhSpecifyMachine);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhSpecifyMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhSpecifyMachine/{id}")
    LhSpecifyMachine getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhSpecifyMachine/checkUnique")
    String checkUnique(@RequestBody LhSpecifyMachine lhSpecifyMachineVO);

    /**
     * 导出硫化定点机台信息列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/lhSpecifyMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody LhSpecifyMachine queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化定点机台信息数据
     */
    @ApiOperation("导入硫化定点机台信息")
    @PostMapping("/lhSpecifyMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
