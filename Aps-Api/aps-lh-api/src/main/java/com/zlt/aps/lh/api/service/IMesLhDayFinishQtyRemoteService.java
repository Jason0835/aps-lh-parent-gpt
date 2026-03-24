package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.MesLhDayFinishQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMesLhDayFinishQtyRemoteService.java
 * 描    述：IMesLhDayFinishQtyRemoteService硫化排程日完成量回报接口前端接口
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMesLhDayFinishQtyRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface IMesLhDayFinishQtyRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mesLhDayFinishQty/list")
    TableDataInfo list(@RequestBody MesLhDayFinishQty QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mesLhDayFinishQty/save")
    AjaxResult save(@RequestBody MesLhDayFinishQty mesLhDayFinishQty);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mesLhDayFinishQty/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mesLhDayFinishQty/{id}")
    MesLhDayFinishQty getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mesLhDayFinishQty/checkUnique")
    String checkUnique(@RequestBody MesLhDayFinishQty mesLhDayFinishQtyVO);

    /**
     * 导出硫化排程日完成量回报接口列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mesLhDayFinishQty/exportData/{fileName}")
    byte[] exportData(@RequestBody MesLhDayFinishQty queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化排程日完成量回报接口数据
     */
    @ApiOperation("导入硫化排程日完成量回报接口")
    @PostMapping("/mesLhDayFinishQty/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
