package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpDemandPlanRemoteService.java
 * 描    述：IDpDemandPlanRemoteService需求计划前端接口
 *@author yelq
 *@date 2025-12-25
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IDpDemandPlanRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE,path = "${api.path.monthplan:/monthplan}")
public interface IDpDemandPlanRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/demandPlan/list")
    TableDataInfo list(@RequestBody DpDemandPlan queryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/demandPlan/save")
    AjaxResult save(@RequestBody DpDemandPlan dpDemandPlan);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/demandPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/demandPlan/{id}")
    DpDemandPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/demandPlan/checkUnique")
    String checkUnique(@RequestBody DpDemandPlan dpDemandPlanVO);

    /**
     * 导出需求计划列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/demandPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody DpDemandPlan queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入需求计划数据
     */
    @ApiOperation("导入需求计划")
    @PostMapping("/demandPlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
    /**
     * 生成需求计划数据
     */
    @ApiOperation("生成需求计划")
    @PostMapping("/demandPlan/createMonthRequire")
    AjaxResult createMonthRequire(@RequestBody  DpDemandPlan createCondition);
    /**
     * 查询需求计划版本号
     */
    @ApiOperation("查询需求计划版本号")
    @PostMapping("/demandPlan/findMonthPlanVersion")
    AjaxResult findMonthPlanVersion(@RequestBody DpDemandPlan queryCondition);
}
