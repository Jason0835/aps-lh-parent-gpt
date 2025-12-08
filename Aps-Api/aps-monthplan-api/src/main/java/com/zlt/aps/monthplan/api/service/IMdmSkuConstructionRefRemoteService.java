package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmSkuConstructionRefRemoteService.java
 * 描    述：IMdmSkuConstructionRefRemoteServiceSKU与施工（示方书）关系前端接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmSkuConstructionRefRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmSkuConstructionRefRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmSkuConstructionRef/list")
    TableDataInfo list(@RequestBody MdmSkuConstructionRef QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmSkuConstructionRef/save")
    AjaxResult save(@RequestBody MdmSkuConstructionRef mdmSkuConstructionRef);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmSkuConstructionRef/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmSkuConstructionRef/{id}")
    MdmSkuConstructionRef getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmSkuConstructionRef/checkUnique")
    String checkUnique(@RequestBody MdmSkuConstructionRef mdmSkuConstructionRefVO);

    /**
     * 导出SKU与施工（示方书）关系列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmSkuConstructionRef/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmSkuConstructionRef queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入SKU与施工（示方书）关系数据
     */
    @ApiOperation("导入SKU与施工（示方书）关系")
    @PostMapping("/mdmSkuConstructionRef/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 抓取MES数据
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mdmSkuConstructionRef/mesCapture")
    AjaxResult mesCapture();


}
