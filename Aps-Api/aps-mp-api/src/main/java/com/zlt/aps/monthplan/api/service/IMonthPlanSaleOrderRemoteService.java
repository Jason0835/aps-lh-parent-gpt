package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanSaleOrder;
import com.zlt.aps.monthplan.api.domain.itf.InSaleOrderDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanSaleOrderRemoteService.java
 * 描    述：IMonthPlanSaleOrderRemoteService月度销售计划订单接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@FeignClient(contextId = "IMonthPlanSaleOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMonthPlanSaleOrderRemoteService {

    /**
     * 查询列表
     *
     * @param QueryVO
     * @return
     */
    @ApiOperation("查询列表")
    @PostMapping("/monthSaleOrderPlan/list")
    TableDataInfo list(@RequestBody MonthPlanSaleOrder QueryVO);

    /**
     * 新增月度销售计划订单
     */
    @ApiOperation("新增月度销售计划订单")
    @PostMapping("/monthSaleOrderPlan/add")
    AjaxResult add(@RequestBody MonthPlanSaleOrder monthPlanSaleOrder);

    /**
     * 修改月度销售计划订单
     */
    @ApiOperation("修改月度销售计划订单")
    @PostMapping("/monthSaleOrderPlan/edit")
    AjaxResult edit(@RequestBody MonthPlanSaleOrder monthPlanSaleOrder);

    /**
     * 校验月度销售计划订单唯一性
     */
    @ApiOperation("校验月度销售计划订单唯一性")
    @PostMapping("/monthSaleOrderPlan/checkMonthPlanSaleOrderUnique")
    String checkMonthPlanSaleOrderUnique(@RequestBody MonthPlanSaleOrder monthPlanSaleOrder);

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @ApiOperation("删除")
    @DeleteMapping("/monthSaleOrderPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出月度销售计划订单列表
     *
     * @param queryVO
     * @param fileName
     * @return
     */
    @ApiOperation("导出列表")
    @PostMapping("/monthSaleOrderPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MonthPlanSaleOrder queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入月度销售计划订单数据
     *
     * @param importContext 导入文件内容
     * @param updateSupport 是否更新
     * @return
     */
    @ApiOperation("导入月度销售计划订单")
    @PostMapping("/monthSaleOrderPlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 内销销售订单同步
     *
     * @param inSaleOrderDto 年月
     * @return 结果
     */
    @PostMapping("/saleOrderSync/syncInSaleOrder")
    public AjaxResult syncInSaleOrder(@RequestBody InSaleOrderDto inSaleOrderDto);

    /**
     * 外销销售订单同步
     *
     * @param inSaleOrderDto 年月
     * @return 结果
     */
    @PostMapping("/saleOrderSync/syncOutSaleOrder")
    public AjaxResult syncOutSaleOrder(@RequestBody InSaleOrderDto inSaleOrderDto);
}
