package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialRatio;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawSpecialMaterialRatioRemoteService.java
 * 描    述：IRawSpecialMaterialRatioRemoteService特殊材料批次比例前端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawSpecialMaterialRatioRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawSpecialMaterialRatioRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawSpecialMaterialRatio/list")
    TableDataInfo list(@RequestBody RawSpecialMaterialRatio QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawSpecialMaterialRatio/save")
    AjaxResult save(@RequestBody RawSpecialMaterialRatio rawSpecialMaterialRatio);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawSpecialMaterialRatio/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawSpecialMaterialRatio/{id}")
    RawSpecialMaterialRatio getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawSpecialMaterialRatio/checkUnique")
    String checkUnique(@RequestBody RawSpecialMaterialRatio rawSpecialMaterialRatioVO);

    /**
     * 导出特殊材料批次比例列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawSpecialMaterialRatio/exportData/{fileName}")
    byte[] exportData(@RequestBody RawSpecialMaterialRatio queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入特殊材料批次比例数据
     */
    @ApiOperation("导入特殊材料批次比例")
    @PostMapping("/rawSpecialMaterialRatio/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
