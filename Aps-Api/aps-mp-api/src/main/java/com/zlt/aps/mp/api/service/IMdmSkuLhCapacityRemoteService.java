package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmSkuLhCapacity;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmSkuLhCapacityRemoteService.java
 * 描    述：IMdmSkuLhCapacityRemoteServiceSKU日硫化产能前端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmSkuLhCapacityRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmSkuLhCapacityRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmSkuLhCapacity/list")
    TableDataInfo list(@RequestBody MdmSkuLhCapacity QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmSkuLhCapacity/save")
    AjaxResult save(@RequestBody MdmSkuLhCapacity mdmSkuLhCapacity);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmSkuLhCapacity/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmSkuLhCapacity/{id}")
    MdmSkuLhCapacity getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmSkuLhCapacity/checkUnique")
    String checkUnique(@RequestBody MdmSkuLhCapacity mdmSkuLhCapacityVO);

    /**
     * 导出SKU日硫化产能列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmSkuLhCapacity/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmSkuLhCapacity queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入SKU日硫化产能数据
     */
    @ApiOperation("导入SKU日硫化产能")
    @PostMapping("/mdmSkuLhCapacity/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 倒算班产，返回计算后的班产
     */
    @ApiOperation("倒算班产，返回计算后的班产")
    @PostMapping("/mdmSkuLhCapacity/getClassCapacity")
    public AjaxResult getClassCapacity(@RequestBody MdmSkuLhCapacity billVO);
}
