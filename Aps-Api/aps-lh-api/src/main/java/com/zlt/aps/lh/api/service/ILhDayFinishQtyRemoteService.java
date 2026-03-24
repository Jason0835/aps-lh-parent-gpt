package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhDayFinishQtyRemoteService.java
 * 描    述：ILhDayFinishQtyRemoteService硫化排程日完成量前端接口
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ILhDayFinishQtyRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhDayFinishQtyRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhDayFinishQty/list")
    TableDataInfo list(@RequestBody LhDayFinishQty QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/lhDayFinishQty/save")
    AjaxResult save(@RequestBody LhDayFinishQty lhDayFinishQty);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhDayFinishQty/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhDayFinishQty/{id}")
    LhDayFinishQty getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhDayFinishQty/checkUnique")
    String checkUnique(@RequestBody LhDayFinishQty lhDayFinishQtyVO);

    /**
     * 导出硫化排程日完成量列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/lhDayFinishQty/exportData/{fileName}")
    byte[] exportData(@RequestBody LhDayFinishQty queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化排程日完成量数据
     */
    @ApiOperation("导入硫化排程日完成量")
    @PostMapping("/lhDayFinishQty/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
