package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mdm.api.domain.entity.CxPrecisionPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxPrecisionPlanRemoteService.java
 * 描    述：成型精度计划前端接口
 *@author APS Team
 *@date 2026-04-03
 *@version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ICxPrecisionPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxPrecisionPlanRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxPrecisionPlan/list")
    TableDataInfo list(@RequestBody CxPrecisionPlan queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/cxPrecisionPlan/save")
    AjaxResult save(@RequestBody CxPrecisionPlan entity);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxPrecisionPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxPrecisionPlan/{id}")
    CxPrecisionPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxPrecisionPlan/checkUnique")
    String checkUnique(@RequestBody CxPrecisionPlan entity);

    /**
     * 导出来型精度计划列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/cxPrecisionPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody CxPrecisionPlan queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/cxPrecisionPlan/importData")
    AjaxResult importData(@RequestBody ImportContext context, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 从MES同步数据生成成型精度初版计划
     */
    @ApiOperation("从MES同步数据生成成型精度初版计划")
    @PostMapping("/cxPrecisionPlan/generateFromMes")
    AjaxResult generatePlansFromMes();

    /**
     * 自动生成年度成型精度计划
     */
    @ApiOperation("自动生成年度成型精度计划")
    @PostMapping("/cxPrecisionPlan/autoGenerateYearly")
    AjaxResult autoGenerateYearlyPlans(@RequestParam("year") Integer year);
}
