package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleStopInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxScheduleStopInfoRemoteService.java
 * 描    述：ICxScheduleStopInfoRemoteService成型机台自动停排信息前端接口
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICxScheduleStopInfoRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxScheduleStopInfoRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxScheduleStopInfo/list")
    TableDataInfo list(@RequestBody CxScheduleStopInfo QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cxScheduleStopInfo/save")
    AjaxResult save(@RequestBody CxScheduleStopInfo cxScheduleStopInfo);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxScheduleStopInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxScheduleStopInfo/{id}")
    CxScheduleStopInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxScheduleStopInfo/checkUnique")
    String checkUnique(@RequestBody CxScheduleStopInfo cxScheduleStopInfoVO);

    /**
     * 导出成型机台自动停排信息列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cxScheduleStopInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody CxScheduleStopInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型机台自动停排信息数据
     */
    @ApiOperation("导入成型机台自动停排信息")
    @PostMapping("/cxScheduleStopInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
