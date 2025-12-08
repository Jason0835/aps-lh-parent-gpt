package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialStock;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawSpecialMaterialStockRemoteService.java
 * 描    述：IRawSpecialMaterialStockRemoteService特殊材料库存前端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawSpecialMaterialStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawSpecialMaterialStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawSpecialMaterialStock/list")
    TableDataInfo list(@RequestBody RawSpecialMaterialStock QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawSpecialMaterialStock/save")
    AjaxResult save(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawSpecialMaterialStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawSpecialMaterialStock/{id}")
    RawSpecialMaterialStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawSpecialMaterialStock/checkUnique")
    String checkUnique(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStockVO);

    /**
     * 导出特殊材料库存列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawSpecialMaterialStock/exportData/{fileName}")
    byte[] exportData(@RequestBody RawSpecialMaterialStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入特殊材料库存数据
     */
    @ApiOperation("导入特殊材料库存")
    @PostMapping("/rawSpecialMaterialStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
