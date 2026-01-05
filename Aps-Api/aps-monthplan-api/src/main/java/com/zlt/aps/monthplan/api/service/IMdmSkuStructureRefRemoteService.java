package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuStructureRef;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmSkuStructureRefRemoteService.java
 * 描    述：IMdmSkuStructureRefRemoteServiceSKU与结构关系前端接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmSkuStructureRefRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmSkuStructureRefRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmSkuStructureRef/list")
    TableDataInfo list(@RequestBody MdmSkuStructureRef QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmSkuStructureRef/save")
    AjaxResult save(@RequestBody MdmSkuStructureRef mdmSkuStructureRef);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmSkuStructureRef/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmSkuStructureRef/{id}")
    MdmSkuStructureRef getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmSkuStructureRef/checkUnique")
    String checkUnique(@RequestBody MdmSkuStructureRef mdmSkuStructureRefVO);

    /**
     * 导出SKU与结构关系列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmSkuStructureRef/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmSkuStructureRef queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入SKU与结构关系数据
     */
    @ApiOperation("导入SKU与结构关系")
    @PostMapping("/mdmSkuStructureRef/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 抓取MES数据
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mdmSkuStructureRef/mesCapture")
    AjaxResult mesCapture();

    /**
     * 查询结构选择列表
     */
    @ApiOperation("查询结构选择列表")
    @PostMapping("/mdmSkuStructureRef/getStructureSelectList")
    public TableDataInfo getStructureSelectList(@RequestBody MdmSkuStructureRef queryVO);

    /**
     * 更新结构到物料表
     * @param queryVO 参数
     * @return 结果
     */
    @ApiOperation("更新结构到物料表")
    @PostMapping("/relation/updateStructureToMaterial")
    public AjaxResult updateStructureToMaterial(@RequestBody MdmSkuStructureRef queryVO);
}
