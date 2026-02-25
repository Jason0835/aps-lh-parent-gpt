package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthPlanStatisticsRemoteService.java
 * 描    述：IMpMonthPlanStatisticsRemoteServiceS2-0612.最终排产计划统计前端接口
 *@author zlt
 *@date 2026-02-05
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpMonthPlanStatisticsRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpMonthPlanStatisticsRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpMonthPlanStatistics/list")
    TableDataInfo list(@RequestBody MpMonthPlanStatistics QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpMonthPlanStatistics/save")
    AjaxResult save(@RequestBody MpMonthPlanStatistics mpMonthPlanStatistics);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpMonthPlanStatistics/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpMonthPlanStatistics/{id}")
    MpMonthPlanStatistics getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpMonthPlanStatistics/checkUnique")
    String checkUnique(@RequestBody MpMonthPlanStatistics mpMonthPlanStatisticsVO);

    /**
     * 导出S2-0612.最终排产计划统计列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpMonthPlanStatistics/exportData/{fileName}")
    byte[] exportData(@RequestBody MpMonthPlanStatistics queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入S2-0612.最终排产计划统计数据
     */
    @ApiOperation("导入S2-0612.最终排产计划统计")
    @PostMapping("/mpMonthPlanStatistics/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
