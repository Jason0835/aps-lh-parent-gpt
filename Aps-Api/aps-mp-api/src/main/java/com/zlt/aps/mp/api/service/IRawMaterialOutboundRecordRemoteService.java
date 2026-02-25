package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.mp.api.domain.entity.RawMaterialOutboundRecord;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawMaterialOutboundRecordRemoteService.java
 * 描    述：IRawMaterialOutboundRecordRemoteService原材料出库量前端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawMaterialOutboundRecordRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawMaterialOutboundRecordRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawMaterialOutboundRecord/list")
    TableDataInfo list(@RequestBody RawMaterialOutboundRecord QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawMaterialOutboundRecord/save")
    AjaxResult save(@RequestBody RawMaterialOutboundRecord rawMaterialOutboundRecord);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawMaterialOutboundRecord/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawMaterialOutboundRecord/{id}")
    RawMaterialOutboundRecord getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawMaterialOutboundRecord/checkUnique")
    String checkUnique(@RequestBody RawMaterialOutboundRecord rawMaterialOutboundRecordVO);

    /**
     * 导出原材料出库量列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawMaterialOutboundRecord/exportData/{fileName}")
    byte[] exportData(@RequestBody RawMaterialOutboundRecord queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入原材料出库量数据
     */
    @ApiOperation("导入原材料出库量")
    @PostMapping("/rawMaterialOutboundRecord/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);



    @ApiOperation("MES抓取")
    @PostMapping("/rawMaterialOutboundRecord/mesCatch")
     AjaxResult mesCatch();
}
