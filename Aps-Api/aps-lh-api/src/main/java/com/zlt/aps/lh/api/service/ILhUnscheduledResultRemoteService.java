package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhUnscheduledResultRemoteService.java
 * 描    述：ILhUnscheduledResultRemoteService硫化未排结果前端接口
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ILhUnscheduledResultRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhUnscheduledResultRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhUnscheduledResult/list")
    TableDataInfo list(@RequestBody LhUnscheduledResult QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/lhUnscheduledResult/save")
    AjaxResult save(@RequestBody LhUnscheduledResult lhUnscheduledResult);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhUnscheduledResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhUnscheduledResult/{id}")
    LhUnscheduledResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhUnscheduledResult/checkUnique")
    String checkUnique(@RequestBody LhUnscheduledResult lhUnscheduledResultVO);

    /**
     * 导出硫化未排结果列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/lhUnscheduledResult/exportData/{fileName}")
    byte[] exportData(@RequestBody LhUnscheduledResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化未排结果数据
     */
    @ApiOperation("导入硫化未排结果")
    @PostMapping("/lhUnscheduledResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
