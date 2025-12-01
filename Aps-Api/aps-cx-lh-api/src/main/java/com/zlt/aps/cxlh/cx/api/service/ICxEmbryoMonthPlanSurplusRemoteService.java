package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.CxEmbryoMonthPlanSurplus;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxEmbryoMonthPlanSurplusRemoteService.java
 * 描    述：ICxEmbryoMonthPlanSurplusRemoteService成型工序胎胚计划量汇总表前端接口
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICxEmbryoMonthPlanSurplusRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:/cxlh}")
public interface ICxEmbryoMonthPlanSurplusRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxEmbryoMonthPlanSurplus/list")
    TableDataInfo list(@RequestBody CxEmbryoMonthPlanSurplus QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cxEmbryoMonthPlanSurplus/save")
    AjaxResult save(@RequestBody CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxEmbryoMonthPlanSurplus/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxEmbryoMonthPlanSurplus/{id}")
    CxEmbryoMonthPlanSurplus getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxEmbryoMonthPlanSurplus/checkUnique")
    String checkUnique(@RequestBody CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplusVO);

    /**
     * 导出成型工序胎胚计划量汇总表列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cxEmbryoMonthPlanSurplus/exportData/{fileName}")
    byte[] exportData(@RequestBody CxEmbryoMonthPlanSurplus queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型工序胎胚计划量汇总表数据
     */
    @ApiOperation("导入成型工序胎胚计划量汇总表")
    @PostMapping("/cxEmbryoMonthPlanSurplus/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
