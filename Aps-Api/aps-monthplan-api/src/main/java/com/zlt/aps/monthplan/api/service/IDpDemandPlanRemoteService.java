package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpDemandPlanRemoteService.java
 * 描    述：IDpDemandPlanRemoteService需求计划前端接口
 *@author yelq
 *@date 2025-12-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IDpDemandPlanRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IDpDemandPlanRemoteService {

    /**
     * 查询需求计划列表
     */
    @ApiOperation("查询需求计划列表")
    @PostMapping("/demandPlan/list")
    TableDataInfo list(@RequestBody DpDemandPlan dpDemandPlan);

    /**
    * 新增需求计划
    */
    @ApiOperation("新增需求计划")
    @PostMapping("/demandPlan/add")
    AjaxResult add(@RequestBody DpDemandPlan dpDemandPlan);

    /**
     * 修改需求计划
     */
    @ApiOperation("修改需求计划")
    @PostMapping("/demandPlan/edit")
    AjaxResult edit(@RequestBody DpDemandPlan dpDemandPlan);

    /**
     * 删除需求计划
     */
    @ApiOperation("删除需求计划")
    @DeleteMapping("/demandPlan/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/demandPlan/{id}")
    DpDemandPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验需求计划唯一性
     */
    @ApiOperation("校验需求计划唯一性")
    @PostMapping("/demandPlan/checkDpDemandPlanUnique")
    String checkDpDemandPlanUnique(@RequestBody DpDemandPlan dpDemandPlan);

    /**
     * 导出需求计划列表
    */
    @ApiOperation("导出需求计划列表")
    @PostMapping("/demandPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody DpDemandPlan dpDemandPlan,@PathVariable("fileName") String fileName);

    /**
     * 导入需求计划数据
     */
    @ApiOperation("导入需求计划")
    @PostMapping("/demandPlan/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
    /**
     * 生成需求计划数据
     */
    @ApiOperation("生成需求计划")
    @PostMapping("/demandPlan/createMonthRequire")
    AjaxResult createMonthRequire(@RequestBody  DpDemandPlan createCondition);
}
