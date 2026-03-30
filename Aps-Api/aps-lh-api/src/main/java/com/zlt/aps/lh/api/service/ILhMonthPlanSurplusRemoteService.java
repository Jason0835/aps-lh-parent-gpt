package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMonthPlanSurplus;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMonthPlanSurplusRemoteService.java
 * 描    述：ILhMonthPlanSurplusRemoteService月度计划外胎汇总前端接口
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ILhMonthPlanSurplusRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMonthPlanSurplusRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhMonthPlanSurplus/list")
    TableDataInfo list(@RequestBody LhMonthPlanSurplus QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/lhMonthPlanSurplus/save")
    AjaxResult save(@RequestBody LhMonthPlanSurplus lhMonthPlanSurplus);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhMonthPlanSurplus/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhMonthPlanSurplus/{id}")
    LhMonthPlanSurplus getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhMonthPlanSurplus/checkUnique")
    String checkUnique(@RequestBody LhMonthPlanSurplus lhMonthPlanSurplusVO);

    /**
     * 导出月度计划外胎汇总列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/lhMonthPlanSurplus/exportData/{fileName}")
    byte[] exportData(@RequestBody LhMonthPlanSurplus queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入月度计划外胎汇总数据
     */
    @ApiOperation("导入月度计划外胎汇总")
    @PostMapping("/lhMonthPlanSurplus/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
