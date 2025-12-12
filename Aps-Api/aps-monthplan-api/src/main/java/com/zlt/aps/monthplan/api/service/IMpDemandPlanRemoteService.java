package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpDemandPlanRemoteService.java
 * 描    述：IMpDemandPlanRemoteService需求计划前端接口
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IMpDemandPlanRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpDemandPlanRemoteService {

    /**
     * 查询需求计划列表
     */
    @ApiOperation("查询需求计划列表")
    @PostMapping("/demandPlan/list")
    TableDataInfo list(@RequestBody MpDemandPlan mpDemandPlan);

    /**
    * 新增需求计划
    */
    @ApiOperation("新增需求计划")
    @PostMapping("/demandPlan/add")
    AjaxResult add(@RequestBody MpDemandPlan mpDemandPlan);

    /**
     * 修改需求计划
     */
    @ApiOperation("修改需求计划")
    @PostMapping("/demandPlan/edit")
    AjaxResult edit(@RequestBody MpDemandPlan mpDemandPlan);

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
    MpDemandPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验需求计划唯一性
     */
    @ApiOperation("校验需求计划唯一性")
    @PostMapping("/demandPlan/checkMpDemandPlanUnique")
    String checkMpDemandPlanUnique(@RequestBody MpDemandPlan mpDemandPlan);

    /**
     * 导出需求计划列表
    */
    @ApiOperation("导出需求计划列表")
    @PostMapping("/demandPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MpDemandPlan mpDemandPlan,@PathVariable("fileName") String fileName);

    /**
     * 导入需求计划数据
     */
    @ApiOperation("导入需求计划")
    @PostMapping("/demandPlan/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
