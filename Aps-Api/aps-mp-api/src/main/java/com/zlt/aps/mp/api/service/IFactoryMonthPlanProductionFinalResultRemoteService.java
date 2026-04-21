package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.dto.FactoryMonthPlanProductionFinalResultParam;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryMonthPlanProductionFinalResultRemoteService.java
 * 描    述：IFactoryMonthPlanProductionFinalResultRemoteService工厂月生产计划-最终排产计划定稿前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@FeignClient(contextId = "IFactoryMonthPlanProductionFinalResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryMonthPlanProductionFinalResultRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/factoryMonthPlanFinalResult/list")
    TableDataInfo list(@RequestBody FactoryMonthPlanProductionFinalResult queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/factoryMonthPlanFinalResult/save")
    AjaxResult save(@RequestBody FactoryMonthPlanProductionFinalResult factoryMonthPlanProductionFinalResult);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/factoryMonthPlanFinalResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/factoryMonthPlanFinalResult/{id}")
    FactoryMonthPlanProductionFinalResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/factoryMonthPlanFinalResult/checkUnique")
    String checkUnique(@RequestBody FactoryMonthPlanProductionFinalResult factoryMonthPlanProductionFinalResultVO);

    /**
     * 导出工厂月生产计划-最终排产计划定稿列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/factoryMonthPlanFinalResult/exportData/{fileName}")
    byte[] exportData(@RequestBody FactoryMonthPlanProductionFinalResult queryVO, @PathVariable("fileName") String fileName);

    
    /**
     * 导出SKU排产明细列表
     */
    @ApiOperation("导出SKU排产明细")
    @PostMapping("/factoryMonthPlanFinalResult/exportSkuScheduleItems/{fileName}")
    byte[] exportSkuScheduleItems(@RequestBody FactoryMonthPlanProductionFinalResult queryVO, @PathVariable("fileName") String fileName);
    
    /**
     * 导入工厂月生产计划-最终排产计划定稿数据
     */
    @ApiOperation("导入工厂月生产计划-最终排产计划定稿")
    @PostMapping("/factoryMonthPlanFinalResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/factoryMonthPlanFinalResult/getVersionList")
    TableDataInfo getVersionList(@RequestBody FactoryMonthPlanProductionFinalResult queryVO);

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/factoryMonthPlanFinalResult/listSkuScheduleItems")
    TableDataInfo listSkuScheduleItems(@RequestBody FactoryMonthPlanProductionFinalResultParam queryVO);

}
