package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMouldChangePlanRemoteService.java
 * 描    述：模具交替计划前端接口
 *@author APS Team
 *@date 2026-04-01
 *@version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ILhMouldChangePlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMouldChangePlanRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhMouldChangePlan/list")
    TableDataInfo list(@RequestBody LhMouldChangePlan queryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/lhMouldChangePlan/save")
    AjaxResult save(@RequestBody LhMouldChangePlan lhMouldChangePlan);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhMouldChangePlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhMouldChangePlan/{id}")
    LhMouldChangePlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhMouldChangePlan/checkUnique")
    String checkUnique(@RequestBody LhMouldChangePlan lhMouldChangePlanVO);

    /**
     * 导出模具交替计划列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhMouldChangePlan/exportData/{fileName}")
    byte[] exportData(@RequestBody LhMouldChangePlan queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模具交替计划数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/lhMouldChangePlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 排程发布
     */
    @ApiOperation("排程发布")
    @PostMapping("/lhMouldChangePlan/issueSchedule")
    AjaxResult issueSchedule(@RequestBody List<Long> ids);

    /**
     * 按查询条件排程发布（仅支持单日排程日期）
     */
    @ApiOperation("按查询条件排程发布")
    @PostMapping("/lhMouldChangePlan/issueScheduleByQuery")
    AjaxResult issueScheduleByQuery(@RequestBody LhMouldChangePlan queryVO);

}
