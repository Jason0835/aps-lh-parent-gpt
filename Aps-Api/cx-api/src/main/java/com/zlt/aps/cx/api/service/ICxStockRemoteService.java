package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxStockRemoteService.java
 * 描    述：成型库存前端接口
 *@author APS Team
 *@date 2026-04-02
 *@version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ICxStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxStock/list")
    TableDataInfo list(@RequestBody CxStock queryVO);

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
    String checkUnique(@RequestBody CxStock cxStock);

    /**
     * 导出成型库存列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/cxStock/exportData/{fileName}")
    byte[] exportData(@RequestBody CxStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型库存数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/cxStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
