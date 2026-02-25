package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustStructureOutRemoteService.java
 * 描    述：IMpAdjustStructureOutRemoteService调整-结构调整记录前端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpAdjustStructureOutRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpAdjustStructureOutRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpAdjustStructureOut/list")
    TableDataInfo list(@RequestBody MpAdjustStructureOut QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpAdjustStructureOut/save")
    AjaxResult save(@RequestBody MpAdjustStructureOut mpAdjustStructureOut);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpAdjustStructureOut/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpAdjustStructureOut/{id}")
    MpAdjustStructureOut getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpAdjustStructureOut/checkUnique")
    String checkUnique(@RequestBody MpAdjustStructureOut mpAdjustStructureOutVO);

    /**
     * 导出调整-结构调整记录列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpAdjustStructureOut/exportData/{fileName}")
    byte[] exportData(@RequestBody MpAdjustStructureOut queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入调整-结构调整记录数据
     */
    @ApiOperation("导入调整-结构调整记录")
    @PostMapping("/mpAdjustStructureOut/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/mpAdjustStructureOut/getVersionList")
    TableDataInfo getVersionList(@RequestBody MpAdjustStructureOut queryVO);


}
