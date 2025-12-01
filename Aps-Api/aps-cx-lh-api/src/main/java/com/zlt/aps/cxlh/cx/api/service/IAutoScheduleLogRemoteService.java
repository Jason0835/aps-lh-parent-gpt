package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.AutoScheduleLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IAutoScheduleLogRemoteService.java
 * 描    述：IAutoScheduleLogRemoteService成型自动排程日志前端接口
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IAutoScheduleLogRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface IAutoScheduleLogRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/autoScheduleLog/list")
    TableDataInfo list(@RequestBody AutoScheduleLog QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/autoScheduleLog/save")
    AjaxResult save(@RequestBody AutoScheduleLog autoScheduleLog);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/autoScheduleLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/autoScheduleLog/{id}")
    AutoScheduleLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/autoScheduleLog/checkUnique")
    String checkUnique(@RequestBody AutoScheduleLog autoScheduleLogVO);

    /**
     * 导出成型自动排程日志列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/autoScheduleLog/exportData/{fileName}")
    byte[] exportData(@RequestBody AutoScheduleLog queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型自动排程日志数据
     */
    @ApiOperation("导入成型自动排程日志")
    @PostMapping("/autoScheduleLog/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
