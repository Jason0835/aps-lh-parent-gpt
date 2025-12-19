package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustStructureInRemoteService.java
 * 描    述：IMpAdjustStructureInRemoteService调整-结构内调整记录前端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpAdjustStructureInRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpAdjustStructureInRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpAdjustStructureIn/list")
    TableDataInfo list(@RequestBody MpAdjustStructureIn QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpAdjustStructureIn/save")
    AjaxResult save(@RequestBody MpAdjustStructureIn mpAdjustStructureIn);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpAdjustStructureIn/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpAdjustStructureIn/{id}")
    MpAdjustStructureIn getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpAdjustStructureIn/checkUnique")
    String checkUnique(@RequestBody MpAdjustStructureIn mpAdjustStructureInVO);

    /**
     * 导出调整-结构内调整记录列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpAdjustStructureIn/exportData/{fileName}")
    byte[] exportData(@RequestBody MpAdjustStructureIn queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入调整-结构内调整记录数据
     */
    @ApiOperation("导入调整-结构内调整记录")
    @PostMapping("/mpAdjustStructureIn/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
