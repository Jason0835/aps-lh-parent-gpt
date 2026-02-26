package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmStructureName;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmStructureNameRemoteService.java
 * 描    述：IMdmStructureNameRemoteService结构信息(SKU与结构关系选择结构使用)前端接口
 *@author zlt
 *@date 2026-02-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmStructureNameRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.mdm:/mdm}")
public interface IMdmStructureNameRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmStructureName/list")
    TableDataInfo list(@RequestBody MdmStructureName QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmStructureName/save")
    AjaxResult save(@RequestBody MdmStructureName mdmStructureName);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmStructureName/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmStructureName/{id}")
    MdmStructureName getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmStructureName/checkUnique")
    String checkUnique(@RequestBody MdmStructureName mdmStructureNameVO);

    /**
     * 导出结构信息(SKU与结构关系选择结构使用)列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmStructureName/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmStructureName queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入结构信息(SKU与结构关系选择结构使用)数据
     */
    @ApiOperation("导入结构信息(SKU与结构关系选择结构使用)")
    @PostMapping("/mdmStructureName/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
