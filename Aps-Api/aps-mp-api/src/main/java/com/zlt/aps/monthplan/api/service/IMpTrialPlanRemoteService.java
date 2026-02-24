package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpTrialPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpTrialPlanRemoteService.java
 * 描    述：IMpTrialPlanRemoteService试制量试计划前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@FeignClient(contextId = "IMpTrialPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpTrialPlanRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpTrialPlan/list")
    TableDataInfo list(@RequestBody MpTrialPlan QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mpTrialPlan/save")
    AjaxResult save(@RequestBody MpTrialPlan mpTrialPlan);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpTrialPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpTrialPlan/{id}")
    MpTrialPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpTrialPlan/checkUnique")
    String checkUnique(@RequestBody MpTrialPlan mpTrialPlanVO);

    /**
     * 导出试制量试计划列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mpTrialPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MpTrialPlan queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入试制量试计划数据
     */
    @ApiOperation("导入试制量试计划")
    @PostMapping("/mpTrialPlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
