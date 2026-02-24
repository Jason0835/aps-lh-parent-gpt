package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldAllocation;
import com.zlt.aps.monthplan.api.domain.vo.PeriodInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMouldAllocationRemoteService.java
 * 描    述：IMdmMouldAllocationRemoteService模具分配比例(同结构/不同结构)前端接口
 *@author zlt
 *@date 2025-12-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmMouldAllocationRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMouldAllocationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmMouldAllocation/list")
    TableDataInfo list(@RequestBody MdmMouldAllocation QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmMouldAllocation/save")
    AjaxResult save(@RequestBody MdmMouldAllocation mdmMouldAllocation);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmMouldAllocation/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMouldAllocation/{id}")
    MdmMouldAllocation getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmMouldAllocation/checkUnique")
    String checkUnique(@RequestBody MdmMouldAllocation mdmMouldAllocationVO);

    /**
     * 导出模具分配比例(同结构/不同结构)列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmMouldAllocation/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMouldAllocation queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模具分配比例(同结构/不同结构)数据
     */
    @ApiOperation("导入模具分配比例(同结构/不同结构)")
    @PostMapping("/mdmMouldAllocation/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 复制模具分配比例
     */
    @PostMapping("/mdmMouldAllocation/copy")
    AjaxResult copy(@RequestBody PeriodInfo periodinfo);

}
