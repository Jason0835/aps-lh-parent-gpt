package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhDispatcherLogRemoteService.java
 * 描    述：ILhDispatcherLogRemoteService硫化调度员排程操作日志前端接口
 *@author zlt
 *@date 2025-03-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ILhDispatcherLogRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhDispatcherLogRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhDispatcherLog/list")
    TableDataInfo list(@RequestBody LhDispatcherLog QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/lhDispatcherLog/save")
    AjaxResult save(@RequestBody LhDispatcherLog lhDispatcherLog);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhDispatcherLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhDispatcherLog/{id}")
    LhDispatcherLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhDispatcherLog/checkUnique")
    String checkUnique(@RequestBody LhDispatcherLog lhDispatcherLogVO);

    /**
     * 导出硫化调度员排程操作日志列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/lhDispatcherLog/exportData/{fileName}")
    byte[] exportData(@RequestBody LhDispatcherLog queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化调度员排程操作日志数据
     */
    @ApiOperation("导入硫化调度员排程操作日志")
    @PostMapping("/lhDispatcherLog/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
