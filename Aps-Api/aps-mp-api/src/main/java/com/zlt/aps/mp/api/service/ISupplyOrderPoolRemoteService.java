package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.SupplyOrderPool;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISupplyOrderPoolRemoteService.java
 * 描    述：ISupplyOrderPoolRemoteService供应链订单池前端接口
 *@author yelq
 *@date 2025-12-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "ISupplyOrderPoolRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface ISupplyOrderPoolRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/supplyOrderPool/list")
    TableDataInfo list(@RequestBody SupplyOrderPool QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/supplyOrderPool/save")
    AjaxResult save(@RequestBody SupplyOrderPool supplyOrderPool);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/supplyOrderPool/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/supplyOrderPool/{id}")
    SupplyOrderPool getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/supplyOrderPool/checkUnique")
    String checkUnique(@RequestBody SupplyOrderPool supplyOrderPoolVO);

    /**
     * 导出供应链订单池列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/supplyOrderPool/exportData/{fileName}")
    byte[] exportData(@RequestBody SupplyOrderPool queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入供应链订单池数据
     */
    @ApiOperation("导入供应链订单池")
    @PostMapping("/supplyOrderPool/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 生成周期排产储备
     */
    @ApiOperation("生成周期排产储备")
    @PostMapping("/supplyOrderPool/createCycleStockUp")
    AjaxResult createCycleStockUp(@RequestBody  SupplyOrderPool supplyOrderPool);
    /**
     * 生成常规储备
     */
    @ApiOperation("生成常规储备")
    @PostMapping("/supplyOrderPool/createPrecedentStockUp")
    AjaxResult createPrecedentStockUp(@RequestBody  SupplyOrderPool supplyOrderPool);
    /**
     * 输入物料编码，带出对应信息
     */
    @ApiOperation("输入物料编码，带出对应信息")
    @PostMapping("/supplyOrderPool/queryRelationByMaterialCode")
    AjaxResult queryRelationByMaterialCode(@RequestBody  SupplyOrderPool supplyOrderPool);
    /**
     * 超期校验
     */
    @ApiOperation("超期校验")
    @PostMapping("/supplyOrderPool/checkOverdue")
    AjaxResult checkOverdue(@RequestBody   SupplyOrderPool supplyOrderPool);

    /**
     * 新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少
     * @param supplyOrderPool 入参
     * @return AjaxResult
     */
    @ApiOperation("新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少")
    @PostMapping("/supplyOrderPool/queryStockUpByMaterialCode")
    public AjaxResult queryStockUpByMaterialCode(@RequestBody SupplyOrderPool supplyOrderPool);
}
