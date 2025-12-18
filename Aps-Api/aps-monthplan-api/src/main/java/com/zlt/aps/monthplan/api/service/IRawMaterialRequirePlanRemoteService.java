package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.monthplan.api.domain.entity.RawMaterialRequirePlan;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawMaterialRequirePlanRemoteService.java
 * 描    述：IRawMaterialRequirePlanRemoteService原材料需求计划前端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawMaterialRequirePlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawMaterialRequirePlanRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawMaterialRequirePlan/list")
    TableDataInfo list(@RequestBody RawMaterialRequirePlan QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawMaterialRequirePlan/save")
    AjaxResult save(@RequestBody RawMaterialRequirePlan rawMaterialRequirePlan);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawMaterialRequirePlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawMaterialRequirePlan/{id}")
    RawMaterialRequirePlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawMaterialRequirePlan/checkUnique")
    String checkUnique(@RequestBody RawMaterialRequirePlan rawMaterialRequirePlanVO);

    /**
     * 导出原材料需求计划列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawMaterialRequirePlan/exportData/{fileName}")
    byte[] exportData(@RequestBody RawMaterialRequirePlan queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入原材料需求计划数据
     */
    @ApiOperation("导入原材料需求计划")
    @PostMapping("/rawMaterialRequirePlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);


    @PostMapping("/rawMaterialRequirePlan/generate")
    @ApiOperation("生成原材料需求计划")
    public AjaxResult generate(@RequestParam String factoryCode,@RequestParam Integer year,
                               @RequestParam Integer month);
}
