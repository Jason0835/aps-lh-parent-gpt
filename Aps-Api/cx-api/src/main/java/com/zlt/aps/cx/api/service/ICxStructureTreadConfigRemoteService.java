package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxStructureTreadConfigRemoteService.java
 * 描    述：胎面整车配置前端接口
 *@author APS Team
 *@date 2026-04-02
 *@version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ICxStructureTreadConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxStructureTreadConfigRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmStructureTreadConfig/list")
    TableDataInfo list(@RequestBody CxStructureTreadConfig queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmStructureTreadConfig/save")
    AjaxResult save(@RequestBody CxStructureTreadConfig cxStructureTreadConfig);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmStructureTreadConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmStructureTreadConfig/{id}")
    CxStructureTreadConfig getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmStructureTreadConfig/checkUnique")
    String checkUnique(@RequestBody CxStructureTreadConfig cxStructureTreadConfig);

    /**
     * 导出胎面整车配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmStructureTreadConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody CxStructureTreadConfig queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入胎面整车配置数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/mdmStructureTreadConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
