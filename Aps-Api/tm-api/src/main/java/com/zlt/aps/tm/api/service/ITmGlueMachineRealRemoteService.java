package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITmGlueMachineRealRemoteService.java
 * 描    述：ITmGlueMachineRealRemoteService胎面胶料与机台关系前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-08
 */
@FeignClient(contextId = "ITmGlueMachineRealRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmGlueMachineRealRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/tmGlueMachineReal/list")
    TableDataInfo list(@RequestBody TmGlueMachineReal QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/tmGlueMachineReal/save")
    AjaxResult save(@RequestBody TmGlueMachineReal tmGlueMachineReal);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/tmGlueMachineReal/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmGlueMachineReal/{id}")
    TmGlueMachineReal getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/tmGlueMachineReal/checkUnique")
    String checkUnique(@RequestBody TmGlueMachineReal tmGlueMachineRealVO);

    /**
     * 导出胎面胶料与机台关系列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/tmGlueMachineReal/exportData/{fileName}")
    byte[] exportData(@RequestBody TmGlueMachineReal queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入胎面胶料与机台关系数据
     */
    @ApiOperation("导入胎面胶料与机台关系")
    @PostMapping("/tmGlueMachineReal/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
