package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.OrderPlanAllocation;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IOrderPlanAllocationRemoteService.java
 * 描    述：IOrderPlanAllocationRemoteService月度销售计划订单分配结果前端接口
 *@author ZLT
 *@date 2025-03-10
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：ZLT
 *     修改内容：...
 */
@FeignClient(contextId = "IOrderPlanAllocationRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IOrderPlanAllocationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/SaleOrderAllocation/list")
    TableDataInfo list(@RequestBody OrderPlanAllocation QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/SaleOrderAllocation/save")
    AjaxResult save(@RequestBody OrderPlanAllocation orderPlanAllocation);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/SaleOrderAllocation/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/SaleOrderAllocation/{id}")
    OrderPlanAllocation getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/SaleOrderAllocation/checkUnique")
    String checkUnique(@RequestBody OrderPlanAllocation orderPlanAllocationVO);

    /**
     * 导出月度销售计划订单分配结果列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/SaleOrderAllocation/exportData/{fileName}")
    byte[] exportData(@RequestBody OrderPlanAllocation queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入月度销售计划订单分配结果数据
     */
    @ApiOperation("导入月度销售计划订单分配结果")
    @PostMapping("/SaleOrderAllocation/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("查询对应年月+分厂的需求计划版本")
    @PostMapping("/SaleOrderAllocation/versionList")
    AjaxResult versionList(@RequestBody OrderPlanAllocation query);

    @ApiOperation("根据查询条件查询统计数据")
    @PostMapping("/SaleOrderAllocation/getSummaryVo")
    AjaxResult getSummaryVo(@RequestBody OrderPlanAllocation orderPlanAllocation);
}
