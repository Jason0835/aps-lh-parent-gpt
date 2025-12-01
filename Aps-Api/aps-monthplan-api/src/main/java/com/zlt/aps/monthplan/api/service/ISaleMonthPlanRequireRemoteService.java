package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISaleMonthPlanRequireRemoteService.java
 * 描    述：ISaleMonthPlanRequireRemoteService月度生产需求计划前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@FeignClient(contextId = "ISaleMonthPlanRequireRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface ISaleMonthPlanRequireRemoteService {

    /**
     * 查询列表
     *
     * @param QueryVO 查询条件
     * @return 返回结果
     */
    @ApiOperation("查询列表")
    @PostMapping("/saleRequireProductionPlan/list")
    TableDataInfo list(@RequestBody SaleMonthPlanRequire QueryVO);

    /**
     * 删除
     *
     * @param ids 删除记录集合
     * @return 返回结果
     */
    @ApiOperation("删除")
    @DeleteMapping("/saleRequireProductionPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出月度生产需求计划列表
     *
     * @param queryVO  查询条件
     * @param fileName 文件名
     * @return 返回结果
     */
    @ApiOperation("导出列表")
    @PostMapping("/saleRequireProductionPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody SaleMonthPlanRequire queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入月度生产需求计划数据
     *
     * @param importContext excel数据对象
     * @param updateSupport 是否更新标识
     * @return 返回结果
     */
    @ApiOperation("导入月度生产需求计划")
    @PostMapping("/saleRequireProductionPlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("查询对应年月+分厂的需求计划版本")
    @PostMapping("/saleRequireProductionPlan/versionList")
    AjaxResult versionList(@RequestBody SaleMonthPlanRequire saleMonthPlanRequire);

    /**
     * 根据条件查询统计数据
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @ApiOperation("根据条件查询统计数据")
    @PostMapping("/saleRequireProductionPlan/getSummaryVo")
    public AjaxResult getSummaryVo(@RequestBody SaleMonthPlanRequire queryVO);
}
