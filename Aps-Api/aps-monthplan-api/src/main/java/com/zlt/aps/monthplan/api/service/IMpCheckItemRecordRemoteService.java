package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpCheckItemRecordRemoteService.java
 * 描    述：IMpCheckItemRecordRemoteServiceS2-1202 检测项记录前端接口
 *@author hsc
 *@date 2026-01-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */
@FeignClient(contextId = "IMpCheckItemRecordRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpCheckItemRecordRemoteService {

    /**
     * 查询S2-1202 检测项记录列表
     */
    @ApiOperation("查询S2-1202 检测项记录列表")
    @PostMapping("/checkItemRecord/list")
    TableDataInfo list(@RequestBody MpCheckItemRecord mpCheckItemRecord);

    /**
    * 新增S2-1202 检测项记录
    */
    @ApiOperation("新增S2-1202 检测项记录")
    @PostMapping("/checkItemRecord/add")
    AjaxResult add(@RequestBody MpCheckItemRecord mpCheckItemRecord);

    /**
     * 修改S2-1202 检测项记录
     */
    @ApiOperation("修改S2-1202 检测项记录")
    @PostMapping("/checkItemRecord/edit")
    AjaxResult edit(@RequestBody MpCheckItemRecord mpCheckItemRecord);

    /**
     * 删除S2-1202 检测项记录
     */
    @ApiOperation("删除S2-1202 检测项记录")
    @DeleteMapping("/checkItemRecord/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/checkItemRecord/{id}")
    MpCheckItemRecord getInfo(@PathVariable("id") Long id);

    /**
     * 校验S2-1202 检测项记录唯一性
     */
    @ApiOperation("校验S2-1202 检测项记录唯一性")
    @PostMapping("/checkItemRecord/checkMpCheckItemRecordUnique")
    String checkMpCheckItemRecordUnique(@RequestBody MpCheckItemRecord mpCheckItemRecord);

    /**
     * 导出S2-1202 检测项记录列表
    */
    @ApiOperation("导出S2-1202 检测项记录列表")
    @PostMapping("/checkItemRecord/exportData/{fileName}")
    byte[] exportData(@RequestBody MpCheckItemRecord mpCheckItemRecord,@PathVariable("fileName") String fileName);

    /**
     * 导入S2-1202 检测项记录数据
     */
    @ApiOperation("导入S2-1202 检测项记录")
    @PostMapping("/checkItemRecord/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
