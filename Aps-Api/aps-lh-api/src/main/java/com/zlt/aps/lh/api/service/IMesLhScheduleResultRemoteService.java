package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.MesLhScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMesLhScheduleResultRemoteService.java
 * 描    述：IMesLhScheduleResultRemoteService硫化排程下发接口前端接口
 *@author zlt
 *@date 2025-03-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMesLhScheduleResultRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface IMesLhScheduleResultRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mesLhScheduleResult/list")
    TableDataInfo list(@RequestBody MesLhScheduleResult QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mesLhScheduleResult/save")
    AjaxResult save(@RequestBody MesLhScheduleResult mesLhScheduleResult);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mesLhScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mesLhScheduleResult/{id}")
    MesLhScheduleResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mesLhScheduleResult/checkUnique")
    String checkUnique(@RequestBody MesLhScheduleResult mesLhScheduleResultVO);

    /**
     * 导出硫化排程下发接口列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mesLhScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody MesLhScheduleResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化排程下发接口数据
     */
    @ApiOperation("导入硫化排程下发接口")
    @PostMapping("/mesLhScheduleResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
