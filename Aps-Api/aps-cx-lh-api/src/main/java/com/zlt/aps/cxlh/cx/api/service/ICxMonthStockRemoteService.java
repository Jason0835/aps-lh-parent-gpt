package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.CxMonthStock;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxMonthStockRemoteService.java
 * 描    述：ICxMonthStockRemoteService成型工序胎胚月结库存前端接口
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICxMonthStockRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:/cxlh}")
public interface ICxMonthStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxMonthStock/list")
    TableDataInfo list(@RequestBody CxMonthStock QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cxMonthStock/save")
    AjaxResult save(@RequestBody CxMonthStock cxMonthStock);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxMonthStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxMonthStock/{id}")
    CxMonthStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxMonthStock/checkUnique")
    String checkUnique(@RequestBody CxMonthStock cxMonthStockVO);

    /**
     * 导出成型工序胎胚月结库存列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cxMonthStock/exportData/{fileName}")
    byte[] exportData(@RequestBody CxMonthStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型工序胎胚月结库存数据
     */
    @ApiOperation("导入成型工序胎胚月结库存")
    @PostMapping("/cxMonthStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
