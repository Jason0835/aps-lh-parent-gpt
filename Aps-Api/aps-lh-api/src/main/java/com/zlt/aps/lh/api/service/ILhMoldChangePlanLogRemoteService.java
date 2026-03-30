package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlanLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMoldChangePlanLogRemoteService.java
 * 描    述：ILhMoldChangePlanLogRemoteService模具变动单日志前端接口
 *@author zlt
 *@date 2025-03-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ILhMoldChangePlanLogRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMoldChangePlanLogRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhMoldChangePlanLog/list")
    TableDataInfo list(@RequestBody LhMoldChangePlanLog QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/lhMoldChangePlanLog/save")
    AjaxResult save(@RequestBody LhMoldChangePlanLog lhMoldChangePlanLog);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhMoldChangePlanLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhMoldChangePlanLog/{id}")
    LhMoldChangePlanLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhMoldChangePlanLog/checkUnique")
    String checkUnique(@RequestBody LhMoldChangePlanLog lhMoldChangePlanLogVO);

    /**
     * 导出模具变动单日志列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/lhMoldChangePlanLog/exportData/{fileName}")
    byte[] exportData(@RequestBody LhMoldChangePlanLog queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模具变动单日志数据
     */
    @ApiOperation("导入模具变动单日志")
    @PostMapping("/lhMoldChangePlanLog/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
