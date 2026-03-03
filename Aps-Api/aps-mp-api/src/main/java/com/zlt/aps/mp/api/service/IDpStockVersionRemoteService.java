package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.DpStockVersion;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmFinishStockRemoteService.java
 * 描    述：IMdmFinishStockRemoteService成品库存前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@FeignClient(contextId = "IDpStockVersionRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IDpStockVersionRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/dpStockVersion/list")
    TableDataInfo list(@RequestBody DpStockVersion QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/dpStockVersion/save")
    AjaxResult save(@RequestBody DpStockVersion dpStockVersion);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/dpStockVersion/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/dpStockVersion/{id}")
    DpStockVersion getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/dpStockVersion/checkUnique")
    String checkUnique(@RequestBody DpStockVersion dpStockVersionVO);

    /**
     * 导出成品库存列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/dpStockVersion/exportData/{fileName}")
    byte[] exportData(@RequestBody DpStockVersion queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成品库存数据
     */
    @ApiOperation("导入成品库存")
    @PostMapping("/dpStockVersion/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 查询需求计划版本号
     */
    @ApiOperation("查询需求计划版本号")
    @PostMapping("/dpStockVersion/findMonthPlanVersion")
    AjaxResult findMonthPlanVersion(@RequestBody DpStockVersion queryCondition);
}
