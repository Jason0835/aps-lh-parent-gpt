package com.zlt.aps.cx.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxScheduleDetailRemoteService.java
 * 描    述：成型排程明细前端接口
 * @author APS Team
 * @date 2026-04-09
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ICxScheduleDetailRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxScheduleDetailRemoteService {

    /**
     * 根据主表ID查询明细列表
     */
    @ApiOperation("根据主表ID查询明细列表")
    @GetMapping("/cxScheduleDetail/listByMainId/{mainId}")
    AjaxResult listByMainId(@PathVariable("mainId") Long mainId);

    /**
     * 根据机台和日期查询明细
     */
    @ApiOperation("根据机台和日期查询明细")
    @GetMapping("/cxScheduleDetail/listByMachineAndDate")
    AjaxResult listByMachineAndDate(
            @RequestParam("cxMachineCode") String cxMachineCode,
            @RequestParam("scheduleDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate);

    /**
     * 根据班次查询明细
     */
    @ApiOperation("根据班次查询明细")
    @GetMapping("/cxScheduleDetail/listByShift")
    AjaxResult listByShift(
            @RequestParam("mainId") Long mainId,
            @RequestParam("shiftCode") String shiftCode);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/cxScheduleDetail/{detailId}")
    AjaxResult getById(@PathVariable("detailId") Long detailId);

    /**
     * 更新完成量
     */
    @ApiOperation("更新完成量")
    @PutMapping("/cxScheduleDetail/updateCompletedQty")
    AjaxResult updateCompletedQty(
            @RequestParam("detailId") Long detailId,
            @RequestParam("completedQuantity") Integer completedQuantity);

    /**
     * 更新车次状态
     */
    @ApiOperation("更新车次状态")
    @PutMapping("/cxScheduleDetail/updateTripStatus")
    AjaxResult updateTripStatus(
            @RequestParam("detailId") Long detailId,
            @RequestParam("tripStatus") String tripStatus);

    /**
     * 修改排程明细
     */
    @ApiOperation("修改排程明细")
    @PutMapping("/cxScheduleDetail/update")
    AjaxResult update(@RequestBody CxScheduleDetail detail);

    /**
     * 删除排程明细（单个）
     */
    @ApiOperation("删除排程明细")
    @DeleteMapping("/cxScheduleDetail/remove/{detailId}")
    AjaxResult remove(@PathVariable("detailId") Long detailId);

    /**
     * 批量删除排程明细
     */
    @ApiOperation("批量删除排程明细")
    @DeleteMapping("/cxScheduleDetail/batchRemove")
    AjaxResult batchRemove(@RequestBody List<Long> detailIds);
}
