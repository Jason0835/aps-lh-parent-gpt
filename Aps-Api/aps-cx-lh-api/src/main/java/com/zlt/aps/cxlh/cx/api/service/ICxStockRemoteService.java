package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxStock;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxStockRemoteService.java
 * 描    述：ICxStockRemoteService成型库存信息前端接口
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICxStockRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxStock/list")
    TableDataInfo list(@RequestBody CxStock QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cxStock/save")
    AjaxResult save(@RequestBody CxStock cxStock);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxStock/{id}")
    CxStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxStock/checkUnique")
    String checkUnique(@RequestBody CxStock cxStockVO);

    /**
     * 导出成型库存信息列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cxStock/exportData/{fileName}")
    byte[] exportData(@RequestBody CxStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型库存信息数据
     */
    @ApiOperation("导入成型库存信息")
    @PostMapping("/cxStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
