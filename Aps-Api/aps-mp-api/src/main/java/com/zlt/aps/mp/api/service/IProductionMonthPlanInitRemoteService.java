package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.ProductionMonthPlanInit;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductionMonthPlanInitRemoteService.java
 * 描    述：IProductionMonthPlanInitRemoteService分厂月生产计划排产过程-计划初始化前端接口
 *@author zlt
 *@date 2025-03-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IProductionMonthPlanInitRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IProductionMonthPlanInitRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/productionMonthPlanInit/list")
    TableDataInfo list(@RequestBody ProductionMonthPlanInit QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/productionMonthPlanInit/save")
    AjaxResult save(@RequestBody ProductionMonthPlanInit productionMonthPlanInit);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/productionMonthPlanInit/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productionMonthPlanInit/{id}")
    ProductionMonthPlanInit getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/productionMonthPlanInit/checkUnique")
    String checkUnique(@RequestBody ProductionMonthPlanInit productionMonthPlanInitVO);

    /**
     * 导出分厂月生产计划排产过程-计划初始化列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/productionMonthPlanInit/exportData/{fileName}")
    byte[] exportData(@RequestBody ProductionMonthPlanInit queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入分厂月生产计划排产过程-计划初始化数据
     */
    @ApiOperation("导入分厂月生产计划排产过程-计划初始化")
    @PostMapping("/productionMonthPlanInit/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
