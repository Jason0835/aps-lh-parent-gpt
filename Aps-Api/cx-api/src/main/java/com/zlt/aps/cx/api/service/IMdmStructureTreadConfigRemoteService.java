package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureTreadConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmStructureTreadConfigRemoteService.java
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
@FeignClient(contextId = "IMdmStructureTreadConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface IMdmStructureTreadConfigRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmStructureTreadConfig/list")
    TableDataInfo list(@RequestBody MdmStructureTreadConfig queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmStructureTreadConfig/save")
    AjaxResult save(@RequestBody MdmStructureTreadConfig mdmStructureTreadConfig);

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
    MdmStructureTreadConfig getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmStructureTreadConfig/checkUnique")
    String checkUnique(@RequestBody MdmStructureTreadConfig mdmStructureTreadConfigVO);

    /**
     * 导出胎面整车配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmStructureTreadConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmStructureTreadConfig queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入胎面整车配置数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/mdmStructureTreadConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
