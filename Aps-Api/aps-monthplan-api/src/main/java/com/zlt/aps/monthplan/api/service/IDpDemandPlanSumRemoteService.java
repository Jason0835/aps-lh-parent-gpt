package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlanSum;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpDemandPlanSumRemoteService.java
 * 描    述：IDpDemandPlanSumRemoteService需求计划汇总前端接口
 *@author yelq
 *@date 2026-01-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IDpDemandPlanSumRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE,path = "${api.path.monthplan:/monthplan}")
public interface IDpDemandPlanSumRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/demandPlanSum/list")
    TableDataInfo list(@RequestBody DpDemandPlanSum QueryVO);
    /**
     * 导出需求计划汇总列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/demandPlanSum/exportData/{fileName}")
    byte[] exportData(@RequestBody DpDemandPlanSum queryVO, @PathVariable("fileName") String fileName);
    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/demandPlanSum/save")
    AjaxResult save(@RequestBody DpDemandPlanSum dpDemandPlanSum);
}
