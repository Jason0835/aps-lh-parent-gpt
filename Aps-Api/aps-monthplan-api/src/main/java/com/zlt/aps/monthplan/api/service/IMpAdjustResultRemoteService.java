package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustResult;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustResultRemoteService.java
 * 描    述：IMpAdjustResultRemoteService调整-调整结果记录前端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpAdjustResultRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpAdjustResultRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpAdjustResult/list")
    TableDataInfo list(@RequestBody MpAdjustResult QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpAdjustResult/save")
    AjaxResult save(@RequestBody MpAdjustResult mpAdjustResult);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpAdjustResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpAdjustResult/{id}")
    MpAdjustResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpAdjustResult/checkUnique")
    String checkUnique(@RequestBody MpAdjustResult mpAdjustResultVO);

    /**
     * 导出调整-调整结果记录列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpAdjustResult/exportData/{fileName}")
    byte[] exportData(@RequestBody MpAdjustResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入调整-调整结果记录数据
     */
    @ApiOperation("导入调整-调整结果记录")
    @PostMapping("/mpAdjustResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/mpAdjustResult/getVersionList")
    TableDataInfo getVersionList(@RequestBody MpAdjustResult queryVO);

}
