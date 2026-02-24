package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthPlanMonitorRemoteService.java
 * 描    述：IMpMonthPlanMonitorRemoteService月度硫化监控前端接口
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpMonthPlanMonitorRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpMonthPlanMonitorRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpMonthPlanMonitor/list")
    TableDataInfo list(@RequestBody MpMonthPlanMonitor QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpMonthPlanMonitor/save")
    AjaxResult save(@RequestBody MpMonthPlanMonitor mpMonthPlanMonitor);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpMonthPlanMonitor/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpMonthPlanMonitor/{id}")
    MpMonthPlanMonitor getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpMonthPlanMonitor/checkUnique")
    String checkUnique(@RequestBody MpMonthPlanMonitor mpMonthPlanMonitorVO);

    /**
     * 导出月度硫化监控列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpMonthPlanMonitor/exportData/{fileName}")
    byte[] exportData(@RequestBody MpMonthPlanMonitor queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入月度硫化监控数据
     */
    @ApiOperation("导入月度硫化监控")
    @PostMapping("/mpMonthPlanMonitor/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
