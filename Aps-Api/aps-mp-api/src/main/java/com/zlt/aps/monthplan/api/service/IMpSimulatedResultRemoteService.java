package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpSimulatedResultRemoteService.java
 * 描    述：IMpSimulatedResultRemoteServiceS2-1004.实单模拟排产前端接口
 *@author yelq
 *@date 2026-01-09
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IMpSimulatedResultRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE,path = "${api.path.monthplan:/monthplan}")
public interface IMpSimulatedResultRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/simulatedResult/list")
    TableDataInfo list(@RequestBody MpSimulatedResult queryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/simulatedResult/save")
    AjaxResult save(@RequestBody MpSimulatedResult mpSimulatedResult);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/simulatedResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/simulatedResult/{id}")
    MpSimulatedResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/simulatedResult/checkUnique")
    String checkUnique(@RequestBody MpSimulatedResult mpSimulatedResultVO);

    /**
     * 导出S2-1004.实单模拟排产列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/simulatedResult/exportData/{fileName}")
    byte[] exportData(@RequestBody MpSimulatedResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入S2-1004.实单模拟排产数据
     */
    @ApiOperation("导入S2-1004.实单模拟排产")
    @PostMapping("/simulatedResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("实单模拟排产")
    @PostMapping("/simulatedResult/createVmMonthPrediction")
    AjaxResult createVmMonthPrediction(@RequestBody MpSimulatedResult queryVO);


}
