package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlanSum;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpDemandPlanSumRemoteService.java
 * 描    述：IDpDemandPlanSumRemoteService需求计划汇总前端接口
 *
 * @author yelq
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：yelq
 * 修改内容：...
 * @date 2026-01-22
 */
@FeignClient(contextId = "IDpDemandPlanSumRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IDpDemandPlanSumRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/demandPlanSum/list")
    TableDataInfo list(@RequestBody DpDemandPlanSum QueryVO);

    /**
     * 查询统计数据
     *
     * @param queryCondition
     * @return
     */
    @ApiOperation("查询统计数据")
    @PostMapping("/demandPlanSum/statisticsInfo")
    AjaxResult statisticsInfo(@RequestBody DpDemandPlanSum queryCondition);

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

    /**
     * 查询需求计划版本号
     */
    @ApiOperation("查询需求计划版本号")
    @PostMapping("/demandPlanSum/findMonthPlanVersion")
    AjaxResult findMonthPlanVersion(@RequestBody DpDemandPlanSum queryCondition);
}
