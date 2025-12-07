package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISupplyOrderPoolRemoteService.java
 * 描    述：ISupplyOrderPoolRemoteService供应链订单池前端接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ISalesOrderPoolRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface ISupplyOrderPoolRemoteService {

    /**
     * 查询供应链订单池列表
     */
    @ApiOperation("查询供应链订单池列表")
    @PostMapping("/supplyOrderPool/list")
    TableDataInfo list(@RequestBody SupplyOrderPool supplyOrderPool);

    /**
    * 新增供应链订单池
    */
    @ApiOperation("新增供应链订单池")
    @PostMapping("/supplyOrderPool/add")
    AjaxResult add(@RequestBody SupplyOrderPool supplyOrderPool);

    /**
     * 修改供应链订单池
     */
    @ApiOperation("修改供应链订单池")
    @PostMapping("/supplyOrderPool/edit")
    AjaxResult edit(@RequestBody SupplyOrderPool supplyOrderPool);

    /**
     * 删除供应链订单池
     */
    @ApiOperation("删除供应链订单池")
    @DeleteMapping("/supplyOrderPool/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/supplyOrderPool/{id}")
    SupplyOrderPool getInfo(@PathVariable("id") Long id);

    /**
     * 校验供应链订单池唯一性
     */
    @ApiOperation("校验供应链订单池唯一性")
    @PostMapping("/supplyOrderPool/checkSupplyOrderPoolUnique")
    String checkSupplyOrderPoolUnique(@RequestBody SupplyOrderPool supplyOrderPool);

    /**
     * 导出供应链订单池列表
    */
    @ApiOperation("导出供应链订单池列表")
    @PostMapping("/supplyOrderPool/exportData/{fileName}")
    byte[] exportData(@RequestBody SupplyOrderPool supplyOrderPool,@PathVariable("fileName") String fileName);

    /**
     * 导入供应链订单池数据
     */
    @ApiOperation("导入供应链订单池")
    @PostMapping("/supplyOrderPool/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
