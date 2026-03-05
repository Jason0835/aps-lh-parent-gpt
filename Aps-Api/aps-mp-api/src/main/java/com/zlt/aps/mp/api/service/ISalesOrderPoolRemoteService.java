package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.SalesOrderPool;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISalesOrderPoolRemoteService.java
 * 描    述：ISalesOrderPoolRemoteService销售订单池前端接口
 *@author zlt
 *@date 2025-12-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ISalesOrderPoolRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface ISalesOrderPoolRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/SalesOrderPool/list")
    TableDataInfo list(@RequestBody SalesOrderPool QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/SalesOrderPool/save")
    AjaxResult save(@RequestBody SalesOrderPool salesOrderPool);


    /**
    * 批量修改同PO号的销售优先级
    */
    @ApiOperation("批量修改同PO号的销售优先级")
    @PostMapping("/SalesOrderPool/editBySalCodePo")
    AjaxResult editBySalCodePo(@RequestBody SalesOrderPool salesOrderPool);

    /**
    * 锁定订单池
    */
    @ApiOperation("锁定订单池")
    @PostMapping("/SalesOrderPool/lockSalesOrderPool")
    AjaxResult lockSalesOrderPool(@RequestBody SalesOrderPool salesOrderPool);

    /**
    * 解锁订单池
    */
    @ApiOperation("解锁订单池")
    @PostMapping("/SalesOrderPool/unlockSalesOrderPool")
    AjaxResult unlockSalesOrderPool(@RequestBody SalesOrderPool salesOrderPool);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/SalesOrderPool/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/SalesOrderPool/{id}")
    SalesOrderPool getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/SalesOrderPool/checkUnique")
    String checkUnique(@RequestBody SalesOrderPool salesOrderPoolVO);

    /**
     * 导出销售订单池列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/SalesOrderPool/exportData/{fileName}")
    byte[] exportData(@RequestBody SalesOrderPool queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入销售订单池数据
     */
    @ApiOperation("导入销售订单池")
    @PostMapping("/SalesOrderPool/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
    * 检查SCM数据
    */
    @ApiOperation("检查SCM数据")
    @PostMapping("/SalesOrderPool/checkSCMData")
    AjaxResult checkSCMData(@RequestBody SalesOrderPool salesOrderPool);


    /**
    * 抓取SCM数据
    */
    @ApiOperation("抓取SCM数据")
    @PostMapping("/SalesOrderPool/getSCMData")
    AjaxResult getSCMData(@RequestBody SalesOrderPool salesOrderPool);

    /**
    * 查询最新两个月的版本锁定情况
    */
    @ApiOperation("查询最新两个月的版本锁定情况")
    @PostMapping("/SalesOrderPool/getMonthLock")
    AjaxResult getMonthLock(@RequestBody SalesOrderPool salesOrderPool);
}
