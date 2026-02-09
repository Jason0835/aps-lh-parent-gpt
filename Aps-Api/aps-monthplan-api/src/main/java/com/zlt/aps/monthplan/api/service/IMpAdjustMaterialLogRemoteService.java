package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustMaterialLog;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustMaterialLogRemoteService.java
 * 描    述：IMpAdjustMaterialLogRemoteService调整-调整日志（未调整及已调整）前端接口
 *@author zlt
 *@date 2026-02-09
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpAdjustMaterialLogRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpAdjustMaterialLogRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpAdjustMaterialLog/list")
    TableDataInfo list(@RequestBody MpAdjustMaterialLog QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpAdjustMaterialLog/save")
    AjaxResult save(@RequestBody MpAdjustMaterialLog mpAdjustMaterialLog);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpAdjustMaterialLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpAdjustMaterialLog/{id}")
    MpAdjustMaterialLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpAdjustMaterialLog/checkUnique")
    String checkUnique(@RequestBody MpAdjustMaterialLog mpAdjustMaterialLogVO);

    /**
     * 导出调整-调整日志（未调整及已调整）列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpAdjustMaterialLog/exportData/{fileName}")
    byte[] exportData(@RequestBody MpAdjustMaterialLog queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入调整-调整日志（未调整及已调整）数据
     */
    @ApiOperation("导入调整-调整日志（未调整及已调整）")
    @PostMapping("/mpAdjustMaterialLog/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
