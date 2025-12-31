package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpProductionPredictionRemoteService.java
 * 描    述：IMpProductionPredictionRemoteServiceS2-1002.未来产量预测前端接口
 *@author yelq
 *@date 2025-12-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IMpProductionPredictionRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE,path = "${api.path.monthplan:/monthplan}")
public interface IMpProductionPredictionRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/productionPrediction/list")
    TableDataInfo list(@RequestBody MpProductionPrediction QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/productionPrediction/save")
    AjaxResult save(@RequestBody MpProductionPrediction mpProductionPrediction);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/productionPrediction/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productionPrediction/{id}")
    MpProductionPrediction getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/productionPrediction/checkUnique")
    String checkUnique(@RequestBody MpProductionPrediction mpProductionPredictionVO);

    /**
     * 导出S2-1002.未来产量预测列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/productionPrediction/exportData/{fileName}")
    byte[] exportData(@RequestBody MpProductionPrediction queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入S2-1002.未来产量预测数据
     */
    @ApiOperation("导入S2-1002.未来产量预测")
    @PostMapping("/productionPrediction/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("生成订单预测")
    @PostMapping("/productionPrediction/createMonthPrediction")
    AjaxResult createMonthPrediction(@RequestBody MpProductionPrediction createCondition);
}
