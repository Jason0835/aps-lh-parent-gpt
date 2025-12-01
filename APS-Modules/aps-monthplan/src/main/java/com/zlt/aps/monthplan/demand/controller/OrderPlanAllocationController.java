package com.zlt.aps.monthplan.demand.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.monthplan.api.domain.entity.OrderPlanAllocation;
import com.zlt.aps.monthplan.demand.service.IOrderPlanAllocationService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：OrderPlanAllocationController.java
 * 描    述：月度销售计划订单分配结果 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-03-10
 */
@Slf4j
@Api(tags = "月度销售计划订单分配结果")
@RestController
@RequestMapping("/SaleOrderAllocation")
public class OrderPlanAllocationController extends BusiController<OrderPlanAllocation> {

    @Autowired
    private IOrderPlanAllocationService orderPlanAllocationService;

    /**
     * 查询月度销售计划订单分配结果列表
     */
    @RequiresPermissions("monthplan:SaleOrderAllocation:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody OrderPlanAllocation queryVO) {
        startPage();
        List<OrderPlanAllocation> list = orderPlanAllocationService.selectList(queryVO);
        return getDataTable(list);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:SaleOrderAllocation:export")
    @Log(title = "月度销售计划订单分配结果", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody OrderPlanAllocation queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(queryVO, fileName, response);
    }

    @ApiOperation("查询对应年月+分厂的需求计划版本")
    @PostMapping("/versionList")
    public AjaxResult versionList(@RequestBody OrderPlanAllocation query) {
        return AjaxResult.success(orderPlanAllocationService.versionList(query));
    }

    @Override
    protected List<OrderPlanAllocation> listExportData(OrderPlanAllocation obj) {
        return orderPlanAllocationService.selectList(obj);
    }

    @ApiOperation("根据查询条件查询统计数据")
    @PostMapping("/getSummaryVo")
    public AjaxResult getSummaryVo(@RequestBody OrderPlanAllocation orderPlanAllocation) {
        return AjaxResult.success(orderPlanAllocationService.getSummaryVo(orderPlanAllocation));
    }
}
