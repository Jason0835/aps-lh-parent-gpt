package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResultLog;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhScheduleResultLogRemoteService.java
 * 描    述：ILhScheduleResultLogRemoteService硫化排程结果日志前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-27
 */
@FeignClient(contextId = "ILhScheduleResultLogRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhScheduleResultLogRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhScheduleResultLog/list")
    TableDataInfo list(@RequestBody LhScheduleResultLog QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/lhScheduleResultLog/save")
    AjaxResult save(@RequestBody LhScheduleResultLog lhScheduleResultLog);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhScheduleResultLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhScheduleResultLog/{id}")
    LhScheduleResultLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhScheduleResultLog/checkUnique")
    String checkUnique(@RequestBody LhScheduleResultLog lhScheduleResultLogVO);

    /**
     * 导出硫化排程结果日志列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhScheduleResultLog/exportData/{fileName}")
    byte[] exportData(@RequestBody LhScheduleResultLog queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化排程结果日志数据
     */
    @ApiOperation("导入硫化排程结果日志")
    @PostMapping("/lhScheduleResultLog/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
